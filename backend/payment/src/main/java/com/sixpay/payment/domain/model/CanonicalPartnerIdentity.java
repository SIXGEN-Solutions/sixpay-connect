package com.sixpay.payment.domain.model;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.Objects;
import java.util.UUID;

/**
 * Canonical SIXPAY Partner identity as consumed by Payment.
 *
 * <p>The authoritative identity is owned and resolved by Partner/Security.
 * Payment stores only the canonical UUID value and has no dependency on
 * Partner internals.</p>
 */
public record CanonicalPartnerIdentity(UUID value) implements ValueObject {

    public CanonicalPartnerIdentity {
        Objects.requireNonNull(value, "Canonical Partner identity is required");
    }

    public static CanonicalPartnerIdentity from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Canonical Partner identity must not be blank"
            );
        }
        return new CanonicalPartnerIdentity(UUID.fromString(value.trim()));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
