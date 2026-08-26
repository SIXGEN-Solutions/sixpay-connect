package com.sixpay.customer.management.domain.model;

import java.util.Objects;
import java.util.UUID;

public record CustomerBankAccountId(UUID value) {

    public CustomerBankAccountId {
        Objects.requireNonNull(value, "value is required");
    }

    public static CustomerBankAccountId from(String value) {
        return new CustomerBankAccountId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
