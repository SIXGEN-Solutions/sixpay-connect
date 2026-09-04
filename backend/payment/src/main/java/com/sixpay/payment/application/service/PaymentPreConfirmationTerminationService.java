package com.sixpay.payment.application.service;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.domain.model.IdempotencyKey;
import com.sixpay.payment.domain.model.PaymentFailure;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.policy.PaymentPolicyBundle;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;

/**
 * Coordinates definitive terminal exits while Payment is still waiting for
 * customer confirmation.
 *
 * <p>The active bank challenge is revoked first. The Payment may become
 * REJECTED or FAILED only after the revocation orchestration reports a stable
 * terminal-safe result.</p>
 */
@Service
@ConditionalOnBean({
        PaymentConfirmationRevocationService.class
})
public class PaymentPreConfirmationTerminationService {

    private final PaymentConfirmationRevocationService revocationService;
    private final PaymentFinalizationService finalizationService;

    public PaymentPreConfirmationTerminationService(
            PaymentConfirmationRevocationService revocationService,
            PaymentFinalizationService finalizationService
    ) {
        this.revocationService = Objects.requireNonNull(
                revocationService,
                "Payment confirmation revocation service"
        );
        this.finalizationService = Objects.requireNonNull(
                finalizationService,
                "Payment finalization service"
        );
    }

    public PaymentWorkflowResult reject(
            PaymentId paymentId,
            PaymentFailure rejection,
            Instant finalizedAt,
            PaymentPolicyBundle policies,
            IdempotencyKey revocationIdempotencyKey,
            CorrelationId correlationId,
            String revocationReasonCode
    ) {
        requireCommon(
                paymentId,
                rejection,
                finalizedAt,
                policies,
                revocationIdempotencyKey,
                correlationId,
                revocationReasonCode
        );

        PaymentConfirmationRevocationService.RevocationResult revocation =
                revocationService.revokeActiveChallenge(
                        paymentId,
                        revocationIdempotencyKey,
                        correlationId,
                        revocationReasonCode
                );

        requireStable(revocation);

        return finalizationService
                .rejectAfterPreConfirmationRevocation(
                        paymentId,
                        rejection,
                        finalizedAt,
                        policies
                );
    }

    public PaymentWorkflowResult failWithoutFinancialEffect(
            PaymentId paymentId,
            PaymentFailure failure,
            Instant finalizedAt,
            PaymentPolicyBundle policies,
            IdempotencyKey revocationIdempotencyKey,
            CorrelationId correlationId,
            String revocationReasonCode
    ) {
        requireCommon(
                paymentId,
                failure,
                finalizedAt,
                policies,
                revocationIdempotencyKey,
                correlationId,
                revocationReasonCode
        );

        PaymentConfirmationRevocationService.RevocationResult revocation =
                revocationService.revokeActiveChallenge(
                        paymentId,
                        revocationIdempotencyKey,
                        correlationId,
                        revocationReasonCode
                );

        requireStable(revocation);

        return finalizationService
                .failAfterPreConfirmationRevocation(
                        paymentId,
                        failure,
                        finalizedAt,
                        policies
                );
    }

    private static void requireCommon(
            PaymentId paymentId,
            PaymentFailure failure,
            Instant finalizedAt,
            PaymentPolicyBundle policies,
            IdempotencyKey idempotencyKey,
            CorrelationId correlationId,
            String reasonCode
    ) {
        Objects.requireNonNull(paymentId, "Payment ID");
        Objects.requireNonNull(failure, "Payment failure");
        Objects.requireNonNull(finalizedAt, "Finalized instant");
        Objects.requireNonNull(policies, "Payment policies");
        Objects.requireNonNull(
                idempotencyKey,
                "Revocation idempotency key"
        );
        Objects.requireNonNull(correlationId, "Correlation ID");
        if (reasonCode == null || reasonCode.isBlank()) {
            throw new IllegalArgumentException(
                    "Revocation reason code must not be blank"
            );
        }
    }

    private static void requireStable(
            PaymentConfirmationRevocationService.RevocationResult result
    ) {
        if (!result.stableForTerminalTransition()) {
            throw new IllegalStateException(
                    "Pre-confirmation terminal transition requires stable revocation"
            );
        }
    }
}
