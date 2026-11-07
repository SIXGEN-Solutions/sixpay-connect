package com.sixpay.payment.infrastructure.idempotency;

import com.sixpay.common.time.TimeProvider;
import com.sixpay.payment.application.command.InitiateDebitCommand;
import com.sixpay.payment.application.port.output.PaymentLookupPort;
import com.sixpay.payment.application.port.output.idempotency
        .PaymentInitiationIdempotencyPort;
import com.sixpay.payment.domain.model.ExternalPaymentReference;
import com.sixpay.payment.application.service
        .PaymentInitiationInProgressException;
import com.sixpay.payment.application.view.InitiateDebitResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * PostgreSQL-backed idempotent execution adapter for InitiateDebit.
 *
 * <p>Initiation idempotency uses independently committed transactions.
 * The banking workflow never runs in a wrapping transaction.</p>
 */
@Component
@ConditionalOnBean({
        PaymentIdempotencyConcurrencyCoordinator.class,
        PaymentIdempotencyReplayStore.class
})
public class PaymentInitiationIdempotencyAdapter
        implements PaymentInitiationIdempotencyPort {

    static final String OPERATION =
            "PAYMENT_INITIATE_DEBIT";

    private final PaymentInitiationCanonicalizer canonicalizer;
    private final PaymentIdempotencyHasher hasher;
    private final PaymentIdempotencyConcurrencyCoordinator coordinator;
    private final PaymentIdempotencyReplayStore replayStore;
    private final PaymentInitiationReplayCodec replayCodec;
    private final PaymentLookupPort paymentLookupPort;
    private final TimeProvider timeProvider;
    private final PaymentInitiationIdempotencyTransactions transactions;

    public PaymentInitiationIdempotencyAdapter(
            PaymentInitiationCanonicalizer canonicalizer,
            PaymentIdempotencyHasher hasher,
            PaymentIdempotencyConcurrencyCoordinator coordinator,
            PaymentIdempotencyReplayStore replayStore,
            PaymentInitiationReplayCodec replayCodec,
            PaymentLookupPort paymentLookupPort,
            TimeProvider timeProvider,
            PaymentInitiationIdempotencyTransactions transactions
    ) {
        this.canonicalizer = Objects.requireNonNull(
                canonicalizer,
                "Payment initiation canonicalizer"
        );

        this.hasher = Objects.requireNonNull(
                hasher,
                "Payment idempotency hasher"
        );

        this.coordinator = Objects.requireNonNull(
                coordinator,
                "Payment idempotency concurrency coordinator"
        );

        this.replayStore = Objects.requireNonNull(
                replayStore,
                "Payment idempotency replay store"
        );

        this.replayCodec = Objects.requireNonNull(
                replayCodec,
                "Payment initiation replay codec"
        );

        this.paymentLookupPort = Objects.requireNonNull(
                paymentLookupPort,
                "Payment lookup port"
        );

        this.timeProvider = Objects.requireNonNull(
                timeProvider,
                "Time provider"
        );
        this.transactions = Objects.requireNonNull(transactions);
    }

    /**
     * Runs the external workflow outside all database transactions; each
     * idempotency decision commits separately. Stale same-key requests are
     * reclaimed only after the grace interval, then resume existing Payment.
     */
    @Override
    @Transactional(propagation = Propagation.NEVER)
    public InitiateDebitResult execute(
            InitiateDebitCommand command,
            Function<String, InitiateDebitResult> newRequest
    ) {
        Objects.requireNonNull(command);
        Objects.requireNonNull(newRequest);
        String hash = hasher.hash(canonicalizer.canonicalize(command));
        PaymentInitiationIdempotencyTransactions.Begin begin = transactions.begin(command, hash);
        return switch (begin.decision().kind()) {
            case REPLAY -> replay(begin.decision());
            case IN_PROGRESS, OUTCOME_UNKNOWN -> throw new PaymentInitiationInProgressException();
            case NEW -> {
                InitiateDebitResult result = newRequest.apply(hash);
                transactions.complete(command, hash, result.paymentId().value(),
                        result.status().name(), replayCodec.encode(result));
                yield result;
            }
        };
    }

    /**
     * Decodes the stored response and verifies that its Payment identifier
     * agrees with the independently stored idempotency metadata before it is
     * returned to the caller.
     */
    private InitiateDebitResult replay(
            PaymentIdempotencyDecision decision
    ) {
        InitiateDebitResult result =
                replayCodec.decode(
                        decision.responsePayload()
                );

        if (!result.paymentId()
                .value()
                .equals(decision.paymentId())) {

            throw new IllegalStateException(
                    "Idempotency replay Payment ID mismatch"
            );
        }

        return result;
    }
}
