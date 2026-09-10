package com.sixpay.payment.application.service;

import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.policy.PostingInstructionIdentity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;

/**
 * Authorizes one posting instruction without calling a banking system.
 */
@Service
public class PaymentPostingPreparationService {

    private final PaymentMutationCoordinator coordinator;

    public PaymentPostingPreparationService(
            PaymentMutationCoordinator coordinator
    ) {
        this.coordinator = Objects.requireNonNull(
                coordinator,
                "Payment mutation coordinator"
        );
    }

    public PaymentWorkflowResult authorizePaymentEventPosting(
            PaymentId paymentId,
            PostingInstructionIdentity instruction,
            Instant authorizedAt
    ) {
        return coordinator.mutate(
                paymentId,
                payment ->
                        payment.authorizePaymentEventPosting(
                                instruction,
                                authorizedAt
                        )
        );
    }

}
