package com.sixpay.payment.domain.policy;

import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.model.evidence.ReversalIdempotencyKey;
import com.sixpay.payment.domain.model.evidence.ReversalInstructionId;

import java.util.Objects;

public record PaymentReversalContext(
        PaymentStatus status,
        ReversalInstructionId instructionId,
        ReversalIdempotencyKey idempotencyKey,
        FinancialEffectKnowledge financialEffectKnowledge
) {
    public PaymentReversalContext {
        status = Objects.requireNonNull(status, "Payment status");
        instructionId = Objects.requireNonNull(
                instructionId,
                "Reversal instruction ID"
        );
        idempotencyKey = Objects.requireNonNull(
                idempotencyKey,
                "Reversal idempotency key"
        );
        financialEffectKnowledge = Objects.requireNonNull(
                financialEffectKnowledge,
                "Financial effect knowledge"
        );
    }
}
