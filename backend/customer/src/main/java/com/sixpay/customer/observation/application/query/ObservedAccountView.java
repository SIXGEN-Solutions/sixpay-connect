package com.sixpay.customer.observation.application.query;

/**
 * Safe account reference exposed by the query model.
 *
 * <p>No raw account number or account binding fingerprint is represented.</p>
 */
public record ObservedAccountView(
        String reference,
        String maskedValue
) {

    public ObservedAccountView {
        reference = requiredText(
                reference,
                128,
                "reference"
        );
        maskedValue = requiredText(
                maskedValue,
                32,
                "maskedValue"
        );
    }

    private static String requiredText(
            String value,
            int maxLength,
            String field
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " must not be blank"
            );
        }

        String normalized = value.strip();

        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    field + " must not exceed "
                            + maxLength
                            + " characters"
            );
        }

        return normalized;
    }
}
