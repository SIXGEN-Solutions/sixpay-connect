package com.sixpay.accounting.api.response;

import com.sixpay.accounting.domain.model.AccountingBatchItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AccountingBatchItemResponse(
        UUID paymentId,
        String publicPaymentReference,
        String partnerId,
        BigDecimal amount,
        String currency,
        Instant paymentOccurredAt,
        LocalDate paymentBusinessDate,
        String bankPostingReference,
        String tresorPayStatus,
        Instant tresorPayStatusCheckedAt,
        String status
) {
    public static AccountingBatchItemResponse from(AccountingBatchItem item) {
        return new AccountingBatchItemResponse(
                item.paymentId(),
                item.publicPaymentReference(),
                item.partnerId(),
                item.amount(),
                item.currency().getCurrencyCode(),
                item.paymentOccurredAt(),
                item.paymentBusinessDate(),
                item.bankPostingReference(),
                item.tresorPayStatus(),
                item.tresorPayStatusCheckedAt(),
                item.status().name()
        );
    }
}
