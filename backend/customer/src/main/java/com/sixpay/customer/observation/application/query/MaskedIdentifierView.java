package com.sixpay.customer.observation.application.query;

/**
 * Purpose-limited masked identifier exposed by query use cases.
 */
public record MaskedIdentifierView(String maskedValue) {

    public MaskedIdentifierView {
        if (maskedValue == null || maskedValue.isBlank()) {
            throw new IllegalArgumentException(
                    "maskedValue must not be blank"
            );
        }

        maskedValue = maskedValue.strip();

        if (maskedValue.length() > 64) {
            throw new IllegalArgumentException(
                    "maskedValue must not exceed 64 characters"
            );
        }
    }
}
