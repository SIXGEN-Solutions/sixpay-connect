package com.sixpay.customer.verification.application.port.output;

import java.util.Objects;

/**
 * Opaque, externally supplied banking lookup reference.
 *
 * @param value protected bank-native reference
 */
public record BankingAccountAccessReference(String value) {

    private static final int MAX_LENGTH = 256;

    public BankingAccountAccessReference {
        Objects.requireNonNull(value, "value is required");
        value = value.strip();

        if (value.isEmpty()) {
            throw new IllegalArgumentException(
                    "Banking account access reference must not be blank"
            );
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Banking account access reference must not exceed 256 characters"
            );
        }
        if (value.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(
                    "Banking account access reference must not contain control characters"
            );
        }
    }

    public static BankingAccountAccessReference of(String value) {
        return new BankingAccountAccessReference(value);
    }

    @Override
    public String toString() {
        return "[PROTECTED_BANKING_REFERENCE]";
    }
}
