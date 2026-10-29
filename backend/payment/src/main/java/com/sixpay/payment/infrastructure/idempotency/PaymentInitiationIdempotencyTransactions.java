package com.sixpay.payment.infrastructure.idempotency;

import com.sixpay.common.time.TimeProvider;
import com.sixpay.payment.application.command.InitiateDebitCommand;
import com.sixpay.payment.application.port.output.PaymentLookupPort;
import com.sixpay.payment.domain.model.ExternalPaymentReference;
import com.sixpay.payment.domain.model.Payment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Short committed decisions. Never invokes banking I/O. */
@Component
public class PaymentInitiationIdempotencyTransactions {
    static final String OPERATION = "PAYMENT_INITIATE_DEBIT";
    // Larger than the normal synchronous initiation deadline. See T0.2 runbook.
    private static final Duration RECOVERY_GRACE = Duration.ofMinutes(2);
    private final PaymentIdempotencyConcurrencyCoordinator locks;
    private final PaymentIdempotencyReplayStore store;
    private final PaymentLookupPort payments;
    private final TimeProvider clock;

    public PaymentInitiationIdempotencyTransactions(
            PaymentIdempotencyConcurrencyCoordinator locks,
            PaymentIdempotencyReplayStore store,
            PaymentLookupPort payments,
            TimeProvider clock) {
        this.locks = Objects.requireNonNull(locks);
        this.store = Objects.requireNonNull(store);
        this.payments = Objects.requireNonNull(payments);
        this.clock = Objects.requireNonNull(clock);
    }

    public record Begin(PaymentIdempotencyDecision decision, boolean resumed) { }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Begin begin(InitiateDebitCommand command, String hash) {
        String partner = command.applicationId();
        return locks.executePartnerPaymentReferenceLocked(partner, command.endToEndId(),
                () -> locks.executeScopedLocked(partner, OPERATION, command.idempotencyKey(),
                        () -> beginLocked(command, hash)));
    }

    private Begin beginLocked(InitiateDebitCommand command, String hash) {
        String partner = command.applicationId();
        Payment existing = payments.findByPartnerIdentifierAndExternalPaymentReference(
                partner, ExternalPaymentReference.of(command.endToEndId())).orElse(null);
        if (existing != null) {
            if (!hash.equals(existing.toState().requestIdentity().requestFingerprint().value())) {
                throw new PaymentExternalReferenceConflictException(partner, command.endToEndId());
            }
            var previous = store.findCompletedReplayForPayment(partner, OPERATION, existing.id().value());
            if (previous.isPresent()) return new Begin(previous.orElseThrow(), false);
            // The original submission key is immutable: different-key recovery must
            // never invent a fresh banking idempotency identity.
            if (!command.idempotencyKey().equals(existing.toState().requestIdentity().idempotencyKey().value())) {
                return new Begin(PaymentIdempotencyDecision.inProgress(), false);
            }
        }
        PaymentIdempotencyDecision decision = store.beginScoped(partner, OPERATION,
                command.idempotencyKey(), hash, clock.now());
        if (decision.kind() == PaymentIdempotencyDecision.Kind.IN_PROGRESS
                || decision.kind() == PaymentIdempotencyDecision.Kind.OUTCOME_UNKNOWN) {
            boolean reclaimed = store.reclaimScopedAfter(partner, OPERATION,
                    command.idempotencyKey(), hash, clock.now().minus(RECOVERY_GRACE), clock.now());
            return new Begin(reclaimed ? PaymentIdempotencyDecision.newRequest() : decision, reclaimed);
        }
        return new Begin(decision, false);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(InitiateDebitCommand command, String hash, UUID paymentId,
                         String status, String payload) {
        locks.executeScopedLocked(command.applicationId(), OPERATION,
                command.idempotencyKey(), () -> {
                    store.completeScoped(command.applicationId(), OPERATION,
                            command.idempotencyKey(), hash, paymentId,
                            status, payload, clock.now());
                    return Boolean.TRUE;
                });
    }
}
