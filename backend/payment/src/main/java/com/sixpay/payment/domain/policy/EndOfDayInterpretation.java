package com.sixpay.payment.domain.policy;

import com.sixpay.payment.domain.model.PaymentFailure;

import java.util.Optional;

public record EndOfDayInterpretation(
        EndOfDayDecision decision,
        PaymentFailure failure
) {
    public EndOfDayInterpretation {
        if (decision == null) {
            throw new NullPointerException("End-of-day decision");
        }
    }

    public Optional<PaymentFailure> failureOptional() {
        return Optional.ofNullable(failure);
    }
}
