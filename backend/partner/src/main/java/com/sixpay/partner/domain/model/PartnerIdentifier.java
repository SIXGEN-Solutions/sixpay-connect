package com.sixpay.partner.domain.model;

import java.util.Objects;

public record PartnerIdentifier(String value) {

    public static final int MAX_LENGTH = 64;

    public PartnerIdentifier {
        Objects.requireNonNull(value, "value is required");
        value = value.strip();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("partnerIdentifier is required");
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "partnerIdentifier must not exceed " + MAX_LENGTH + " characters"
            );
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
