package com.sixpay.payment.domain.policy;

import com.sixpay.payment.domain.model.PaymentFailure;

import java.util.Optional;

public record ReversalOutcomeInterpretation(
        ReversalDecision decision,
        PaymentFailure failure
) {
    public ReversalOutcomeInterpretation {
        if (decision == null) {
            throw new NullPointerException("Reversal decision");
        }
    }

    public Optional<PaymentFailure> failureOptional() {
        return Optional.ofNullable(failure);
    }
}
