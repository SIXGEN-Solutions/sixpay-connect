package com.sixpay.payment.application.service;

import com.sixpay.payment.domain.model.PaymentFailure;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.TreasuryAccountReference;
import com.sixpay.payment.domain.model.evidence.TreasuryAccountResolutionSnapshot;
import com.sixpay.payment.domain.policy.PaymentPolicyBundle;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;

/**
 * Coordinates Treasury-account resolution evidence.
 */
@Service
public class PaymentTreasuryResolutionService {

    private final PaymentMutationCoordinator coordinator;

    public PaymentTreasuryResolutionService(
            PaymentMutationCoordinator coordinator
    ) {
        this.coordinator = Objects.requireNonNull(
                coordinator,
                "Payment mutation coordinator"
        );
    }

    public PaymentWorkflowResult recordResolution(
            PaymentId paymentId,
            TreasuryAccountResolutionSnapshot evidence,
            TreasuryAccountReference resolvedAccount,
            PaymentFailure failure,
            Instant decisionAt,
            PaymentPolicyBundle policies
    ) {
        return coordinator.mutate(
                paymentId,
                payment ->
                        payment.recordTreasuryAccountResolution(
                                evidence,
                                resolvedAccount,
                                failure,
                                decisionAt,
                                policies
                        )
        );
    }
}
