package com.sixpay.payment.application.service;

import com.sixpay.payment.application.port.output.PaymentLookupPort;
import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.model.financial.PaymentFinancialEventSnapshot;
import com.sixpay.payment.domain.policy.PostingInstructionIdentity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

/**
 * Final durable T0 reservation. No Core Banking HTTP calls inside this transaction.
 * The Payment event, its two frozen financial entries and posting authorization
 * participate in the same transaction manager (verify in integration tests).
 */
@Service
public class PaymentT0PostingReservationService {

    private final PaymentLookupPort paymentLookupPort;
    private final PaymentT0SnapshotService snapshotService;
    private final PaymentPostingPreparationService postingPreparationService;

    public PaymentT0PostingReservationService(
            PaymentLookupPort paymentLookupPort,
            PaymentT0SnapshotService snapshotService,
            PaymentPostingPreparationService postingPreparationService
    ) {
        this.paymentLookupPort = Objects.requireNonNull(paymentLookupPort);
        this.snapshotService = Objects.requireNonNull(snapshotService);
        this.postingPreparationService = Objects.requireNonNull(postingPreparationService);
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentFinancialEventSnapshot reserve(
            PaymentId paymentId,
            PostingInstructionIdentity instruction,
            Instant requestedAt
    ) {
        Objects.requireNonNull(paymentId);
        Objects.requireNonNull(instruction);
        Objects.requireNonNull(requestedAt);
        Payment payment = paymentLookupPort.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        if (payment.status() != PaymentStatus.APPROVED_FOR_POSTING) {
            throw new ReservationLostException();
        }
        var treasury = payment.toState().treasuryAccountReference().orElseThrow();
        var snapshot = snapshotService.getOrCreateFinalized(payment, treasury, requestedAt);
        if (!instruction.amount().equals(snapshot.requestedAmount())
                || !instruction.instructionFingerprint().equals(
                        PaymentT0FinancialRequestFingerprint.from(snapshot))) {
            throw new IllegalStateException("Posting instruction conflicts with frozen Events snapshot");
        }
        var result = postingPreparationService.authorizePaymentEventPosting(
                paymentId, instruction, requestedAt);
        if (!result.stateChanged()) {
            // Important: exception forces rollback of newly inserted event/entries.
            throw new ReservationLostException();
        }
        return snapshot;
    }

    public static final class ReservationLostException extends RuntimeException {
        public ReservationLostException() {
            super("T0 posting authorization was already claimed by another execution");
        }
    }
}
