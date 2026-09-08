package com.sixpay.accounting.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record AccountingPaymentCandidate(
        UUID paymentId,
        String publicPaymentReference,
        String partnerId,
        String financialInstitutionCode,
        UUID financialSnapshotId,
        String financialSnapshotVersion,
        Instant financialSnapshotFinalizedAt,
        String debtorAccountReference,
        String creditorAccountReference,
        BigDecimal amount,
        Currency currency,
        Instant paymentOccurredAt,
        LocalDate paymentBusinessDate,
        String bankPostingReference,
        TresorPayPaymentStatusEvidence tresorPayStatusEvidence,
        List<FrozenEntry> entries
) {
    public AccountingPaymentCandidate {
        paymentId = nonNil(paymentId, "paymentId");
        publicPaymentReference = required(publicPaymentReference, "publicPaymentReference");
        partnerId = required(partnerId, "partnerId");
        financialInstitutionCode = required(financialInstitutionCode, "financialInstitutionCode");
        financialSnapshotId = nonNil(financialSnapshotId, "financialSnapshotId");
        financialSnapshotVersion = required(financialSnapshotVersion, "financialSnapshotVersion");
        financialSnapshotFinalizedAt = Objects.requireNonNull(
                financialSnapshotFinalizedAt,
                "financialSnapshotFinalizedAt"
        );
        debtorAccountReference = required(debtorAccountReference, "debtorAccountReference");
        creditorAccountReference = required(creditorAccountReference, "creditorAccountReference");
        amount = Objects.requireNonNull(amount, "amount");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        currency = Objects.requireNonNull(currency, "currency");
        paymentOccurredAt = Objects.requireNonNull(paymentOccurredAt, "paymentOccurredAt");
        paymentBusinessDate = Objects.requireNonNull(paymentBusinessDate, "paymentBusinessDate");
        bankPostingReference = required(bankPostingReference, "bankPostingReference");
        tresorPayStatusEvidence = Objects.requireNonNull(
                tresorPayStatusEvidence,
                "tresorPayStatusEvidence"
        );
        entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("frozen financial entries must not be empty");
        }
    }

    public record FrozenEntry(
            UUID entrySnapshotId,
            int sequence,
            String direction,
            String accountReference,
            BigDecimal amount,
            Currency currency,
            Instant createdAt
    ) {
        public FrozenEntry {
            entrySnapshotId = nonNil(entrySnapshotId, "entrySnapshotId");
            if (sequence <= 0) {
                throw new IllegalArgumentException("entry sequence must be positive");
            }
            direction = required(direction, "direction");
            if (!"DEBIT".equals(direction) && !"CREDIT".equals(direction)) {
                throw new IllegalArgumentException("direction must be DEBIT or CREDIT");
            }
            accountReference = required(accountReference, "accountReference");
            amount = Objects.requireNonNull(amount, "amount");
            if (amount.signum() <= 0) {
                throw new IllegalArgumentException("entry amount must be positive");
            }
            currency = Objects.requireNonNull(currency, "currency");
            createdAt = Objects.requireNonNull(createdAt, "createdAt");
        }
    }

    private static UUID nonNil(UUID value, String name) {
        value = Objects.requireNonNull(value, name);
        if (value.equals(new UUID(0L, 0L))) {
            throw new IllegalArgumentException(name + " must not be nil");
        }
        return value;
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.strip();
    }
}
