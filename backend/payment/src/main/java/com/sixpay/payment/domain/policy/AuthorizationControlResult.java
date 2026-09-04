package com.sixpay.payment.domain.policy;

import java.util.Objects;

public record AuthorizationControlResult(
        AuthorizationControl control,
        AuthorizationControlOutcome outcome,
        String reason
) {
    public AuthorizationControlResult {
        control = Objects.requireNonNull(control, "Control");
        outcome = Objects.requireNonNull(outcome, "Outcome");
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "Authorization control reason must not be blank"
            );
        }
    }
}
