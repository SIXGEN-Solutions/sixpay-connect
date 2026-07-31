package com.sixpay.payment.domain.policy;

import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.model.evidence.ReversalInstructionId;

import java.util.Objects;

public record PaymentReversalEligibilityContext(
        PaymentStatus status,
        FinancialEffectKnowledge financialEffectKnowledge,
        ReversalInstructionId currentInstructionId
) {
    public PaymentReversalEligibilityContext {
        status = Objects.requireNonNull(status, "Payment status");
        financialEffectKnowledge = Objects.requireNonNull(
                financialEffectKnowledge,
                "Financial effect knowledge"
        );
    }
}
