package com.sixpay.accounting.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
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
        bankPostingReference = bankPostingReference == null || bankPostingReference.isBlank()
                ? null : bankPostingReference.strip();
        tresorPayStatus = required(tresorPayStatus, "tresorPayStatus");
        tresorPayStatusCheckedAt = Objects.requireNonNull(
                tresorPayStatusCheckedAt, "tresorPayStatusCheckedAt"
        );
        status = Objects.requireNonNull(status, "status");
    }

    public static AccountingBatchItem from(AccountingPaymentCandidate candidate) {
        return new AccountingBatchItem(
                candidate.paymentId(),
                candidate.publicPaymentReference(),
                candidate.partnerId(),
                candidate.amount(),
                candidate.currency(),
                candidate.paymentOccurredAt(),
                candidate.paymentBusinessDate(),
                candidate.bankPostingReference(),
                candidate.tresorPayStatusEvidence().providerStatus(),
                candidate.tresorPayStatusEvidence().checkedAt(),
                AccountingBatchItemStatus.PENDING
        );
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.strip();
    }
}
