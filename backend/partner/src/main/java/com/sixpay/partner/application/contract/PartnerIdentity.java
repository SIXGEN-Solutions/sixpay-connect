package com.sixpay.partner.application.contract;

import java.util.Objects;
import java.util.UUID;

/** Minimal public canonical SIXPAY Partner identity contract. */
public record PartnerIdentity(UUID value) {
    public PartnerIdentity {
        Objects.requireNonNull(value, "Partner identity is required");
    }

    public static PartnerIdentity from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Partner identity must not be blank");
        }
        return new PartnerIdentity(UUID.fromString(value.trim()));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
