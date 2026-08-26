package com.sixpay.payment.application.service;

import com.sixpay.payment.domain.model.PaymentFailure;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.evidence.FundsControlSnapshot;
import com.sixpay.payment.domain.policy.PaymentPolicyBundle;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;

/**
 * Coordinates accepted funds-control evidence.
 */
@Service
public class PaymentFundsControlService {

    private final PaymentMutationCoordinator coordinator;

    public PaymentFundsControlService(
            PaymentMutationCoordinator coordinator
    ) {
        this.coordinator = Objects.requireNonNull(
                coordinator,
                "Payment mutation coordinator"
        );
    }

    public PaymentWorkflowResult recordFundsControl(
            PaymentId paymentId,
            FundsControlSnapshot evidence,
            PaymentFailure failure,
            Instant decisionAt,
            PaymentPolicyBundle policies
    ) {
        return coordinator.mutate(
                paymentId,
                payment ->
                        payment.recordFundsControl(
                                evidence,
                                failure,
                                decisionAt,
                                policies
                        )
        );
    }
}
