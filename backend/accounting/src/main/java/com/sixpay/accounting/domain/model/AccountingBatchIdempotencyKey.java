package com.sixpay.accounting.domain.model;

public record AccountingBatchIdempotencyKey(String value) {
    public AccountingBatchIdempotencyKey {
        if (value == null || !value.matches("^[a-f0-9]{64}$")) {
            throw new IllegalArgumentException(
                    "Accounting batch idempotency key must be a lowercase SHA-256 hex value"
            );
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
