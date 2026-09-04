package com.sixpay.payment.application.service;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.application.confirmation.PaymentConfirmationChallengeFactory;
import com.sixpay.payment.application.port.output.PaymentLookupPort;
import com.sixpay.payment.application.port.output.banking.BankingIdempotencyKey;
import com.sixpay.payment.application.port.output.banking.BankingRequestContext;
import com.sixpay.payment.application.port.output.banking.PaymentConfirmationBankResult;
import com.sixpay.payment.application.port.output.banking.PaymentConfirmationGateway;
import com.sixpay.payment.application.port.output.idempotency.PaymentConfirmationIdempotencyPort;
import com.sixpay.payment.application.port.output.idempotency.PaymentConfirmationIdempotencyResult;
import com.sixpay.payment.domain.model.ConfirmationChallenge;
import com.sixpay.payment.domain.model.ConfirmationChallengeStatus;
import com.sixpay.payment.domain.model.IdempotencyKey;
import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PaymentStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;

/**
 * Internal application orchestration for revoking the current active
 * Payment-confirmation challenge before a definitive pre-confirmation exit.
 *
 * <p>This service never exposes a public endpoint and never terminalizes the
 * Payment. LOT 1.9.3 owns the guarded terminal transition after a stable
 * revocation decision.</p>
 */
@Service
@ConditionalOnBean({
        PaymentConfirmationGateway.class,
        PaymentConfirmationIdempotencyPort.class
})
public class PaymentConfirmationRevocationService {

    private final PaymentLookupPort paymentLookupPort;
    private final PaymentAuthorizationService authorizationService;
    private final PaymentConfirmationGateway confirmationGateway;
    private final PaymentConfirmationIdempotencyPort idempotencyPort;

    public PaymentConfirmationRevocationService(
            PaymentLookupPort paymentLookupPort,
            PaymentAuthorizationService authorizationService,
            PaymentConfirmationGateway confirmationGateway,
            PaymentConfirmationIdempotencyPort idempotencyPort
    ) {
        this.paymentLookupPort = Objects.requireNonNull(
                paymentLookupPort,
                "Payment lookup port"
        );
        this.authorizationService = Objects.requireNonNull(
                authorizationService,
                "Payment authorization service"
        );
        this.confirmationGateway = Objects.requireNonNull(
                confirmationGateway,
                "Payment confirmation gateway"
        );
        this.idempotencyPort = Objects.requireNonNull(
                idempotencyPort,
                "Payment confirmation idempotency port"
        );
    }

    /**
     * Revokes the current ACTIVE challenge and persists the authoritative
     * REVOKED snapshot.
     *
     * <p>Already-REVOKED is replay-safe and requires no bank call. Any status
     * other than ACTIVE or REVOKED is rejected, including VERIFIED.</p>
     */
    public RevocationResult revokeActiveChallenge(
            PaymentId paymentId,
            IdempotencyKey idempotencyKey,
            CorrelationId correlationId,
            String reasonCode
    ) {
        Objects.requireNonNull(paymentId, "Payment ID");
        Objects.requireNonNull(idempotencyKey, "Idempotency key");
        Objects.requireNonNull(correlationId, "Correlation ID");

        String effectiveReasonCode =
                requireReasonCode(reasonCode);

        Payment payment = paymentLookupPort
                .findById(paymentId)
                .orElseThrow(() ->
                        new PaymentNotFoundException(paymentId)
                );

        requirePendingConfirmation(payment);

        ConfirmationChallenge current =
                payment.toState()
                        .confirmationChallenge()
                        .orElse(null);

        if (current == null) {
            return RevocationResult.noChallenge(
                    payment.id(),
                    payment.publicPaymentReference()
            );
        }

        if (current.status()
                == ConfirmationChallengeStatus.REVOKED) {
            return RevocationResult.alreadyRevoked(
                    payment.id(),
                    payment.publicPaymentReference(),
                    current
            );
        }

        if (!current.active()) {
            throw new IllegalStateException(
                    "Only ACTIVE confirmation challenge may be revoked"
            );
        }

        BankingRequestContext context =
                new BankingRequestContext(
                        correlationId,
                        payment.toState().financialInstitutionCode()
                );
        BankingIdempotencyKey bankKey =
                new BankingIdempotencyKey(
                        idempotencyKey.value()
                );

        PaymentConfirmationIdempotencyResult idempotencyResult =
                idempotencyPort.executeRevoke(
                        payment.id(),
                        payment.publicPaymentReference(),
                        current.challengeReference(),
                        idempotencyKey,
                        effectiveReasonCode,
                        () -> confirmationGateway.revoke(
                                new PaymentConfirmationGateway.RevokeRequest(
                                        payment.id(),
                                        payment.publicPaymentReference(),
                                        current.challengeReference(),
                                        context,
                                        bankKey,
                                        effectiveReasonCode
                                )
                        ),
                        () -> confirmationGateway.recover(
                                new PaymentConfirmationGateway.RecoveryRequest(
                                        context,
                                        bankKey
                                )
                        )
                );

        PaymentConfirmationBankResult bankResult =
                idempotencyResult.result();

        requireStableRevokedResult(
                current,
                bankResult
        );

        ConfirmationChallenge revoked =
                PaymentConfirmationChallengeFactory.fromBankResult(
                        bankResult,
                        current.binding()
                );

        authorizationService.attachConfirmationChallenge(
                payment.id(),
                revoked,
                observationInstant(payment, bankResult)
        );

        return RevocationResult.revoked(
                payment.id(),
                payment.publicPaymentReference(),
                revoked,
                idempotencyResult.replayed()
        );
    }

    private static void requirePendingConfirmation(
            Payment payment
    ) {
        if (payment.status()
                != PaymentStatus.PENDING_CONFIRMATION) {
            throw new IllegalStateException(
                    "Confirmation revocation requires "
                            + "PENDING_CONFIRMATION"
            );
        }
    }

    private static String requireReasonCode(
            String reasonCode
    ) {
        if (reasonCode == null
                || reasonCode.isBlank()) {
            throw new IllegalArgumentException(
                    "Revocation reason code must not be blank"
            );
        }
        return reasonCode;
    }

    private static void requireStableRevokedResult(
            ConfirmationChallenge current,
            PaymentConfirmationBankResult result
    ) {
        Objects.requireNonNull(
                result,
                "Payment confirmation revoke result"
        );

        if (!current.challengeReference().equals(
                result.challengeReference()
        )) {
            throw new IllegalStateException(
                    "Revocation result challenge reference mismatch"
            );
        }

        if (result.status()
                != ConfirmationChallengeStatus.REVOKED) {
            throw new IllegalStateException(
                    "Stable revocation requires REVOKED challenge status"
            );
        }
    }

    private static Instant observationInstant(
            Payment payment,
            PaymentConfirmationBankResult result
    ) {
        Instant updatedAt =
                payment.toState().updatedAt();
        Instant sentAt =
                result.sentAt();

        if (sentAt == null
                || sentAt.isBefore(updatedAt)) {
            return updatedAt;
        }
        return sentAt;
    }

    public record RevocationResult(
            PaymentId paymentId,
            com.sixpay.payment.domain.model.PublicPaymentReference
                    paymentReference,
            ConfirmationChallenge challenge,
            boolean bankOperationPerformed,
            boolean replayed
    ) {

        public RevocationResult {
            paymentId = Objects.requireNonNull(
                    paymentId,
                    "Payment ID"
            );
            paymentReference = Objects.requireNonNull(
                    paymentReference,
                    "Payment reference"
            );
        }

        static RevocationResult noChallenge(
                PaymentId paymentId,
                com.sixpay.payment.domain.model.PublicPaymentReference
                        paymentReference
        ) {
            return new RevocationResult(
                    paymentId,
                    paymentReference,
                    null,
                    false,
                    false
            );
        }

        static RevocationResult alreadyRevoked(
                PaymentId paymentId,
                com.sixpay.payment.domain.model.PublicPaymentReference
                        paymentReference,
                ConfirmationChallenge challenge
        ) {
            return new RevocationResult(
                    paymentId,
                    paymentReference,
                    challenge,
                    false,
                    true
            );
        }

        static RevocationResult revoked(
                PaymentId paymentId,
                com.sixpay.payment.domain.model.PublicPaymentReference
                        paymentReference,
                ConfirmationChallenge challenge,
                boolean replayed
        ) {
            return new RevocationResult(
                    paymentId,
                    paymentReference,
                    challenge,
                    true,
                    replayed
            );
        }

        public boolean stableForTerminalTransition() {
            return challenge == null
                    || challenge.status()
                    == ConfirmationChallengeStatus.REVOKED;
        }
    }
}
