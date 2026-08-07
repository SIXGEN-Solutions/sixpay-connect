package com.sixpay.accounting.infrastructure.accountingapi.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AccountingBatchRequestDto(
        UUID batchId,
        String idempotencyKey,
        LocalDate businessDate,
        String financialInstitutionCode,
        Instant createdAt,
        List<Item> items
) {
    public record Item(
            UUID paymentId,
            String publicPaymentReference,
            String partnerId,
            BigDecimal amount,
            String currency,
            Instant paymentOccurredAt,
            LocalDate paymentBusinessDate,
            String bankPostingReference,
            String tresorPayStatus,
            Instant tresorPayStatusCheckedAt
    ) {
    }
}
