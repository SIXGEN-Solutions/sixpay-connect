package com.sixpay.customer.verification.domain.model;

import com.sixpay.customer.verification.domain.exception.CustomerVerificationDomainException;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.Objects;
import java.util.UUID;

/**
 * Stable identifier of one Customer Verification attempt.
 *
 * @param value externally generated UUID
 */
public record CustomerVerificationId(UUID value) implements ValueObject {

    private static final UUID NIL_UUID = new UUID(0L, 0L);

    public CustomerVerificationId {
        value = Objects.requireNonNull(value, "value is required");
        if (NIL_UUID.equals(value)) {
            throw new CustomerVerificationDomainException(
                    "Customer verification ID must not be the nil UUID"
            );
        }
    }

    public static CustomerVerificationId from(String value) {
        if (value == null || value.isBlank()) {
            throw new CustomerVerificationDomainException(
                    "Customer verification ID is required"
            );
        }
        try {
            return new CustomerVerificationId(
                    UUID.fromString(value.strip())
            );
        } catch (IllegalArgumentException exception) {
            throw new CustomerVerificationDomainException(
                    "Customer verification ID must be a valid UUID"
            );
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
