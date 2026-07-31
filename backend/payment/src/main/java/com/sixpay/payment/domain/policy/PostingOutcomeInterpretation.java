package com.sixpay.payment.domain.policy;

import com.sixpay.payment.domain.model.PaymentFailure;

import java.util.Optional;

public record PostingOutcomeInterpretation(
        PostingDecision decision,
        PaymentFailure failure
) {
    public PostingOutcomeInterpretation {
        if (decision == null) {
            throw new NullPointerException("Posting decision");
        }
    }

    public Optional<PaymentFailure> failureOptional() {
        return Optional.ofNullable(failure);
    }
}
