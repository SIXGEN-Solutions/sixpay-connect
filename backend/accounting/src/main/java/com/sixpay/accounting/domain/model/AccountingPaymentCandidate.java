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
        BigDecimal amount,
        Currency currency,
        Instant paymentOccurredAt,
        LocalDate paymentBusinessDate,
        String bankPostingReference,
        UUID financialSnapshotId,
        String financialSnapshotVersion,
        Instant financialSnapshotFinalizedAt,
        String debtorAccountReference,
        String creditorAccountReference,
        List<FrozenEntry> frozenEntries,
        TresorPayPaymentStatusEvidence tresorPayStatusEvidence
) {
    public AccountingPaymentCandidate {
        paymentId = Objects.requireNonNull(paymentId, "paymentId");
        if (paymentId.equals(new UUID(0L, 0L))) {
            throw new IllegalArgumentException("paymentId must not be nil");
        }
        publicPaymentReference = required(publicPaymentReference, "publicPaymentReference");
        partnerId = required(partnerId, "partnerId");
        financialInstitutionCode = required(financialInstitutionCode, "financialInstitutionCode");
        amount = Objects.requireNonNull(amount, "amount");
        if (amount.signum() <= 0) throw new IllegalArgumentException("amount must be positive");
        currency = Objects.requireNonNull(currency, "currency");
        paymentOccurredAt = Objects.requireNonNull(paymentOccurredAt, "paymentOccurredAt");
        paymentBusinessDate = Objects.requireNonNull(paymentBusinessDate, "paymentBusinessDate");
        bankPostingReference = optional(bankPostingReference);

        if (financialSnapshotId != null) {
            financialSnapshotVersion = required(financialSnapshotVersion, "financialSnapshotVersion");
            financialSnapshotFinalizedAt = Objects.requireNonNull(financialSnapshotFinalizedAt, "financialSnapshotFinalizedAt");
            debtorAccountReference = required(debtorAccountReference, "debtorAccountReference");
            creditorAccountReference = required(creditorAccountReference, "creditorAccountReference");
            frozenEntries = List.copyOf(Objects.requireNonNull(frozenEntries, "frozenEntries"));
            if (frozenEntries.size() != 2) {
                throw new IllegalArgumentException("finalized financial snapshot must contain exactly two frozen entries");
            }
        } else {
            financialSnapshotVersion = null;
            financialSnapshotFinalizedAt = null;
            debtorAccountReference = null;
            creditorAccountReference = null;
            frozenEntries = frozenEntries == null ? List.of() : List.copyOf(frozenEntries);
        }

        tresorPayStatusEvidence = Objects.requireNonNull(tresorPayStatusEvidence, "tresorPayStatusEvidence");
    }

    public AccountingPaymentCandidate(
            UUID paymentId,
            String publicPaymentReference,
            String partnerId,
            String financialInstitutionCode,
            BigDecimal amount,
            Currency currency,
            Instant paymentOccurredAt,
            LocalDate paymentBusinessDate,
            String bankPostingReference,
            TresorPayPaymentStatusEvidence tresorPayStatusEvidence
    ) {
        this(paymentId, publicPaymentReference, partnerId, financialInstitutionCode,
                amount, currency, paymentOccurredAt, paymentBusinessDate, bankPostingReference,
                null, null, null, null, null, List.of(), tresorPayStatusEvidence);
    }

    public boolean hasFinalizedFinancialSnapshot() {
        return financialSnapshotId != null
                && financialSnapshotVersion != null
                && financialSnapshotFinalizedAt != null
                && debtorAccountReference != null
                && creditorAccountReference != null
                && frozenEntries.size() == 2;
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
            entrySnapshotId = Objects.requireNonNull(entrySnapshotId, "entrySnapshotId");
            if (sequence <= 0) throw new IllegalArgumentException("sequence must be positive");
            direction = required(direction, "direction");
            if (!"DEBIT".equals(direction) && !"CREDIT".equals(direction)) {
                throw new IllegalArgumentException("direction must be DEBIT or CREDIT");
            }
            accountReference = required(accountReference, "accountReference");
            amount = Objects.requireNonNull(amount, "amount");
            if (amount.signum() <= 0) throw new IllegalArgumentException("entry amount must be positive");
            currency = Objects.requireNonNull(currency, "currency");
            createdAt = Objects.requireNonNull(createdAt, "createdAt");
        }
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        return value.strip();
    }

    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
