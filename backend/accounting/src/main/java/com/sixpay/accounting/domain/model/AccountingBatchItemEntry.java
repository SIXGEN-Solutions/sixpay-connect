package com.sixpay.accounting.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.Objects;
import java.util.UUID;

public record AccountingBatchItemEntry(
        UUID entrySnapshotId,
        int sequence,
        String direction,
        String accountReference,
        BigDecimal amount,
        Currency currency,
        Instant createdAt
) {
    public AccountingBatchItemEntry {
        entrySnapshotId = Objects.requireNonNull(entrySnapshotId, "entrySnapshotId");
        if (sequence <= 0) throw new IllegalArgumentException("sequence must be positive");
        direction = required(direction, "direction");
        if (!"DEBIT".equals(direction) && !"CREDIT".equals(direction)) {
            throw new IllegalArgumentException("direction must be DEBIT or CREDIT");
        }
        accountReference = required(accountReference, "accountReference");
        amount = Objects.requireNonNull(amount, "amount");
        if (amount.signum() <= 0) throw new IllegalArgumentException("amount must be positive");
        currency = Objects.requireNonNull(currency, "currency");
        createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        return value.strip();
    }
}
