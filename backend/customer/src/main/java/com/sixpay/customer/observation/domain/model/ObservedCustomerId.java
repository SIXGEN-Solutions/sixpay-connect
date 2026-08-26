package com.sixpay.customer.observation.domain.model;

import com.sixpay.customer.observation.domain.exception.ObservedCustomerDomainException;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.Objects;
import java.util.UUID;

public record ObservedCustomerId(UUID value) implements ValueObject {

    private static final UUID NIL_UUID = new UUID(0L, 0L);

    public ObservedCustomerId {
        value = Objects.requireNonNull(value, "observedCustomerId is required");
        if (NIL_UUID.equals(value)) {
            throw new ObservedCustomerDomainException(
                    "observedCustomerId must not be nil"
            );
        }
    }

    public static ObservedCustomerId of(UUID value) {
        return new ObservedCustomerId(value);
    }
}
