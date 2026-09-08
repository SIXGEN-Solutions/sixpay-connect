package com.sixpay.accounting.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record AccountingBatchItem(
        UUID paymentId,
        String publicPaymentReference,
        String partnerId,
        BigDecimal amount,
        Currency currency,
        Instant paymentOccurredAt,
        LocalDate paymentBusinessDate,
        String bankPostingReference,
        String tresorPayStatus,
        Instant tresorPayStatusCheckedAt,
        AccountingBatchItemStatus status,
        UUID financialSnapshotId,
        String financialSnapshotVersion,
        Instant financialSnapshotFinalizedAt,
        String debtorAccountReference,
        String creditorAccountReference,
        List<AccountingBatchItemEntry> entries
) {
    public AccountingBatchItem {
        paymentId = Objects.requireNonNull(paymentId, "paymentId");
        publicPaymentReference = required(publicPaymentReference, "publicPaymentReference");
        partnerId = required(partnerId, "partnerId");
        amount = Objects.requireNonNull(amount, "amount");
        if (amount.signum() <= 0) throw new IllegalArgumentException("amount must be positive");
        currency = Objects.requireNonNull(currency, "currency");
        paymentOccurredAt = Objects.requireNonNull(paymentOccurredAt, "paymentOccurredAt");
        paymentBusinessDate = Objects.requireNonNull(paymentBusinessDate, "paymentBusinessDate");
        bankPostingReference = optional(bankPostingReference);
        tresorPayStatus = required(tresorPayStatus, "tresorPayStatus");
        tresorPayStatusCheckedAt = Objects.requireNonNull(tresorPayStatusCheckedAt, "tresorPayStatusCheckedAt");
        status = Objects.requireNonNull(status, "status");
        financialSnapshotVersion = optional(financialSnapshotVersion);
        debtorAccountReference = optional(debtorAccountReference);
        creditorAccountReference = optional(creditorAccountReference);
        entries = List.copyOf(entries == null ? List.of() : entries);

        boolean any = financialSnapshotId != null || financialSnapshotVersion != null
                || financialSnapshotFinalizedAt != null || debtorAccountReference != null
                || creditorAccountReference != null || !entries.isEmpty();
        boolean all = financialSnapshotId != null && financialSnapshotVersion != null
                && financialSnapshotFinalizedAt != null && debtorAccountReference != null
                && creditorAccountReference != null && !entries.isEmpty();
        if (any && !all) throw new IllegalArgumentException("financial snapshot evidence must be complete");
        if (all && entries.size() != 2) {
            throw new IllegalArgumentException("T1 batch item must contain exactly two frozen entries");
        }
    }

    /** Legacy constructor for pre-T1.3 persisted batches/tests only. */
    public AccountingBatchItem(
            UUID paymentId,
            String publicPaymentReference,
            String partnerId,
            BigDecimal amount,
            Currency currency,
            Instant paymentOccurredAt,
            LocalDate paymentBusinessDate,
            String bankPostingReference,
            String tresorPayStatus,
            Instant tresorPayStatusCheckedAt,
            AccountingBatchItemStatus status
    ) {
        this(paymentId, publicPaymentReference, partnerId, amount, currency,
                paymentOccurredAt, paymentBusinessDate, bankPostingReference,
                tresorPayStatus, tresorPayStatusCheckedAt, status,
                null, null, null, null, null, List.of());
    }

    public static AccountingBatchItem from(AccountingCandidateProjection candidate) {
        Objects.requireNonNull(candidate, "candidate");
        var evidence = Objects.requireNonNull(candidate.tresorPayStatusEvidence(), "tresorPayStatusEvidence");
        return new AccountingBatchItem(
                candidate.paymentId(), candidate.publicPaymentReference(), candidate.partnerId(),
                candidate.amount(), candidate.currency(), candidate.paymentOccurredAt(),
                candidate.accountingBusinessDate(), candidate.bankReference(), evidence.providerStatus(),
                evidence.checkedAt(), AccountingBatchItemStatus.PENDING,
                candidate.financialSnapshotId(), candidate.financialSnapshotVersion(),
                candidate.financialSnapshotFinalizedAt(), candidate.debtorAccountReference(),
                candidate.creditorAccountReference(), candidate.entries().stream()
                        .map(entry -> new AccountingBatchItemEntry(
                                entry.entrySnapshotId(), entry.sequence(), entry.direction(),
                                entry.accountReference(), entry.amount(), entry.currency(), entry.createdAt()))
                        .toList());
    }

    public static AccountingBatchItem from(AccountingPaymentCandidate candidate) {
        return new AccountingBatchItem(
                candidate.paymentId(), candidate.publicPaymentReference(), candidate.partnerId(),
                candidate.amount(), candidate.currency(), candidate.paymentOccurredAt(),
                candidate.paymentBusinessDate(), candidate.bankPostingReference(),
                candidate.tresorPayStatusEvidence().providerStatus(),
                candidate.tresorPayStatusEvidence().checkedAt(), AccountingBatchItemStatus.PENDING);
    }

    public boolean hasFinancialSnapshotEvidence() { return financialSnapshotId != null; }

    public AccountingBatchItem withStatus(AccountingBatchItemStatus newStatus) {
        return new AccountingBatchItem(
                paymentId, publicPaymentReference, partnerId, amount, currency,
                paymentOccurredAt, paymentBusinessDate, bankPostingReference,
                tresorPayStatus, tresorPayStatusCheckedAt,
                Objects.requireNonNull(newStatus, "newStatus"), financialSnapshotId,
                financialSnapshotVersion, financialSnapshotFinalizedAt,
                debtorAccountReference, creditorAccountReference, entries);
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        return value.strip();
    }

    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
