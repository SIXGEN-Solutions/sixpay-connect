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
        UUID financialSnapshotId,
        String financialSnapshotVersion,
        Instant financialSnapshotFinalizedAt,
        String debtorAccountReference,
        String creditorAccountReference,
        List<FrozenEntry> frozenEntries,
        String tresorPayStatus,
        Instant tresorPayStatusCheckedAt,
        AccountingBatchItemStatus status
) {
    public AccountingBatchItem {
        paymentId = Objects.requireNonNull(paymentId, "paymentId");
        publicPaymentReference = required(publicPaymentReference, "publicPaymentReference");
        partnerId = required(partnerId, "partnerId");
        amount = Objects.requireNonNull(amount, "amount");
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
                throw new IllegalArgumentException("T1.3 batch item must contain exactly two frozen financial entries");
            }
        } else {
            financialSnapshotVersion = null;
            financialSnapshotFinalizedAt = null;
            debtorAccountReference = null;
            creditorAccountReference = null;
            frozenEntries = frozenEntries == null ? List.of() : List.copyOf(frozenEntries);
        }

        tresorPayStatus = required(tresorPayStatus, "tresorPayStatus");
        tresorPayStatusCheckedAt = Objects.requireNonNull(tresorPayStatusCheckedAt, "tresorPayStatusCheckedAt");
        status = Objects.requireNonNull(status, "status");
    }

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
                null, null, null, null, null, List.of(),
                tresorPayStatus, tresorPayStatusCheckedAt, status);
    }

    public static AccountingBatchItem from(AccountingPaymentCandidate candidate) {
        if (!candidate.hasFinalizedFinancialSnapshot()) {
            throw new IllegalArgumentException("T1.3 batch constitution requires a finalized financial snapshot");
        }
        return new AccountingBatchItem(
                candidate.paymentId(), candidate.publicPaymentReference(), candidate.partnerId(),
                candidate.amount(), candidate.currency(), candidate.paymentOccurredAt(),
                candidate.paymentBusinessDate(), candidate.bankPostingReference(),
                candidate.financialSnapshotId(), candidate.financialSnapshotVersion(),
                candidate.financialSnapshotFinalizedAt(), candidate.debtorAccountReference(),
                candidate.creditorAccountReference(),
                candidate.frozenEntries().stream()
                        .map(e -> new FrozenEntry(e.entrySnapshotId(), e.sequence(), e.direction(),
                                e.accountReference(), e.amount(), e.currency(), e.createdAt()))
                        .toList(),
                candidate.tresorPayStatusEvidence().providerStatus(),
                candidate.tresorPayStatusEvidence().checkedAt(),
                AccountingBatchItemStatus.PENDING
        );
    }

    public AccountingBatchItem withStatus(AccountingBatchItemStatus newStatus) {
        return new AccountingBatchItem(
                paymentId, publicPaymentReference, partnerId, amount, currency,
                paymentOccurredAt, paymentBusinessDate, bankPostingReference,
                financialSnapshotId, financialSnapshotVersion, financialSnapshotFinalizedAt,
                debtorAccountReference, creditorAccountReference, frozenEntries,
                tresorPayStatus, tresorPayStatusCheckedAt, newStatus
        );
    }

    public record FrozenEntry(
            UUID entrySnapshotId,
            int sequence,
            String direction,
            String accountReference,
            BigDecimal amount,
            Currency currency,
            Instant createdAt
    ) {}

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        return value.strip();
    }

    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
