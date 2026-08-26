package com.sixpay.payment.application.port.output.banking;

import java.util.Objects;

public record BankingIdempotencyKey(String value) {

    public BankingIdempotencyKey {
        Objects.requireNonNull(value, "Banking idempotency key");
        if (value.isBlank() || value.length() > 150) {
            throw new IllegalArgumentException(
                    "Banking idempotency key must be non-blank "
                            + "and at most 150 characters"
            );
        }
    }
}
