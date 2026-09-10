package com.sixpay.accounting.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record PaymentT0FinalizedForAccounting(
        UUID eventId,
        Instant occurredAt,
        int schemaVersion,
        UUID paymentId,
        String publicPaymentReference,
        String partnerId,
        String financialInstitutionCode,
        String paymentStatus,
        String t0Outcome,
        String bankReference,
        Instant t0ObservedAt,
        LocalDate accountingBusinessDate,
        UUID financialSnapshotId,
        String financialSnapshotVersion,
        Instant financialSnapshotFinalizedAt,
        String debtorAccountReference,
        String creditorAccountReference,
        BigDecimal amount,
        Currency currency,
        Instant paymentOccurredAt,
        List<Entry> entries
) {
    public PaymentT0FinalizedForAccounting {
        eventId = nonNil(eventId, "eventId");
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        if (schemaVersion != 1) throw new IllegalArgumentException("schemaVersion must be 1");
        paymentId = nonNil(paymentId, "paymentId");
        publicPaymentReference = required(publicPaymentReference, "publicPaymentReference");
        partnerId = required(partnerId, "partnerId");
        financialInstitutionCode = required(financialInstitutionCode, "financialInstitutionCode");
        paymentStatus = required(paymentStatus, "paymentStatus");
        if (!"POSTED_PENDING_TFJ".equals(paymentStatus)) throw new IllegalArgumentException("paymentStatus must be POSTED_PENDING_TFJ");
        t0Outcome = required(t0Outcome, "t0Outcome");
        if (!"COMPLETED".equals(t0Outcome)) throw new IllegalArgumentException("t0Outcome must be COMPLETED");
        bankReference = required(bankReference, "bankReference");
        t0ObservedAt = Objects.requireNonNull(t0ObservedAt, "t0ObservedAt");
        accountingBusinessDate = Objects.requireNonNull(accountingBusinessDate, "accountingBusinessDate");
        financialSnapshotId = nonNil(financialSnapshotId, "financialSnapshotId");
        financialSnapshotVersion = required(financialSnapshotVersion, "financialSnapshotVersion");
        financialSnapshotFinalizedAt = Objects.requireNonNull(financialSnapshotFinalizedAt, "financialSnapshotFinalizedAt");
        debtorAccountReference = required(debtorAccountReference, "debtorAccountReference");
        creditorAccountReference = required(creditorAccountReference, "creditorAccountReference");
        amount = Objects.requireNonNull(amount, "amount");
        if (amount.signum() <= 0) throw new IllegalArgumentException("amount must be positive");
        currency = Objects.requireNonNull(currency, "currency");
        paymentOccurredAt = Objects.requireNonNull(paymentOccurredAt, "paymentOccurredAt");
        entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
        if (entries.size() != 2) throw new IllegalArgumentException("finalized T0 snapshot must contain exactly two frozen entries");
    }

    public record Entry(
            UUID entrySnapshotId,
            int sequence,
            String direction,
            String accountReference,
            BigDecimal amount,
            Currency currency,
            Instant createdAt
    ) {
        public Entry {
            entrySnapshotId = nonNil(entrySnapshotId, "entrySnapshotId");
            if (sequence <= 0) throw new IllegalArgumentException("sequence must be positive");
            direction = required(direction, "direction");
            if (!"DEBIT".equals(direction) && !"CREDIT".equals(direction)) throw new IllegalArgumentException("direction must be DEBIT or CREDIT");
            accountReference = required(accountReference, "accountReference");
            amount = Objects.requireNonNull(amount, "amount");
            if (amount.signum() <= 0) throw new IllegalArgumentException("entry amount must be positive");
            currency = Objects.requireNonNull(currency, "currency");
            createdAt = Objects.requireNonNull(createdAt, "createdAt");
        }
    }

    private static UUID nonNil(UUID value, String name) {
        value = Objects.requireNonNull(value, name);
        if (value.equals(new UUID(0L, 0L))) throw new IllegalArgumentException(name + " must not be nil");
        return value;
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        return value.strip();
    }
}
