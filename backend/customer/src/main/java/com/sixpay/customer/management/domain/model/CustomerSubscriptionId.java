package com.sixpay.customer.management.domain.model;

import java.util.Objects;
import java.util.UUID;

public record CustomerSubscriptionId(UUID value) {

    public CustomerSubscriptionId {
        Objects.requireNonNull(value, "value is required");
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
