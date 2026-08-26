package com.sixpay.accounting.domain.model;

import java.util.Objects;
import java.util.UUID;

public record AccountingBatchId(UUID value) {
    public AccountingBatchId {
        value = Objects.requireNonNull(value, "Accounting batch ID");
        if (value.equals(new UUID(0L, 0L))) {
            throw new IllegalArgumentException("Accounting batch ID must not be nil");
        }
    }

    public static AccountingBatchId newId() {
        return new AccountingBatchId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
