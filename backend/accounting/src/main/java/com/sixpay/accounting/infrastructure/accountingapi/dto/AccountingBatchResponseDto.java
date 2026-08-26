package com.sixpay.accounting.infrastructure.accountingapi.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AccountingBatchResponseDto(
        UUID batchId,
        String idempotencyKey,
        String status,
        String providerBatchReference,
        Instant processedAt,
        List<Item> items
) {
    public record Item(
            UUID paymentId,
            String status,
            String providerItemReference,
            String rejectionCode
    ) {
    }
}
