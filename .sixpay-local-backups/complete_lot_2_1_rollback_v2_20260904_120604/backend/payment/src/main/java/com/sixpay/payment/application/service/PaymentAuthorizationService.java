package com.sixpay.payment.application.service;

import com.sixpay.payment.domain.model.PaymentFailure;
import com.sixpay.payment.domain.model.ConfirmationChallenge;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.authorization.SixpayAuthorizationDecisionSnapshot;
import com.sixpay.payment.domain.model.evidence.BankingVerificationSnapshot;
import com.sixpay.payment.domain.policy.PaymentPolicyBundle;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;

/**
 * Coordinates the SIXPAY-local authorization gate and banking-verification
 * evidence.
 */
@Service
public class PaymentAuthorizationService {

    private final PaymentMutationCoordinator coordinator;

    public PaymentAuthorizationService(
            PaymentMutationCoordinator coordinator
    ) {
        this.coordinator = Objects.requireNonNull(
                coordinator,
                "Payment mutation coordinator"
        );
    }

    /**
     * Persists the current bank-issued confirmation challenge while the
     * Payment remains PENDING_CONFIRMATION.
     */
    public PaymentWorkflowResult attachConfirmationChallenge(
            PaymentId paymentId,
            ConfirmationChallenge challenge,
            Instant observedAt
    ) {
        Objects.requireNonNull(
                challenge,
                "Confirmation challenge"
        );
        Objects.requireNonNull(
                observedAt,
                "Confirmation challenge observation instant"
        );
        return coordinator.mutate(
                paymentId,
                payment -> payment.recordConfirmationChallenge(
                        challenge,
                        observedAt
                )
        );
    }

    public PaymentWorkflowResult startAuthorization(
            PaymentId paymentId,
            ConfirmationChallenge verifiedChallenge
    ) {
        Objects.requireNonNull(
                verifiedChallenge,
                "Verified confirmation challenge"
        );
        return coordinator.mutate(
                paymentId,
                payment -> payment.recordCustomerConfirmation(
                        verifiedChallenge
                )
        );
    }

    public PaymentWorkflowResult startAuthorization(
            PaymentId paymentId,
            Instant startedAt
    ) {
        return coordinator.mutate(
                paymentId,
                payment ->
                        payment.startAuthorizationChecking(
                                startedAt
                        )
        );
    }

    public PaymentWorkflowResult recordAuthorizationDecision(
            PaymentId paymentId,
            SixpayAuthorizationDecisionSnapshot decision,
            PaymentFailure rejectionFailure,
            Instant decisionAt,
            PaymentPolicyBundle policies
    ) {
        return coordinator.mutate(
                paymentId,
                payment ->
                        payment.recordAuthorizationDecision(
                                decision,
                                rejectionFailure,
                                decisionAt,
                                policies
                        )
        );
    }

    public PaymentWorkflowResult recordBankingVerification(
            PaymentId paymentId,
            BankingVerificationSnapshot evidence,
            PaymentFailure failure,
            Instant decisionAt,
            PaymentPolicyBundle policies
    ) {
        return coordinator.mutate(
                paymentId,
                payment ->
                        payment.recordBankingVerification(
                                evidence,
                                failure,
                                decisionAt,
                                policies
                        )
        );
    }
}
