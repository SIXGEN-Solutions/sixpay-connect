package com.sixpay.accounting.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record AccountingBatch(
        AccountingBatchId batchId,
        AccountingBatchIdempotencyKey idempotencyKey,
        LocalDate businessDate,
        String financialInstitutionCode,
        Instant createdAt,
        AccountingBatchStatus status,
        List<AccountingBatchItem> items
) {
    public AccountingBatch {
        batchId = Objects.requireNonNull(batchId, "batchId");
        idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        businessDate = Objects.requireNonNull(businessDate, "businessDate");
        if (financialInstitutionCode == null || financialInstitutionCode.isBlank()) {
            throw new IllegalArgumentException("financialInstitutionCode is required");
        }
        financialInstitutionCode = financialInstitutionCode.strip();
        createdAt = Objects.requireNonNull(createdAt, "createdAt");
        status = Objects.requireNonNull(status, "status");
        items = List.copyOf(Objects.requireNonNull(items, "items"));
        if (items.isEmpty()) {
            throw new IllegalArgumentException(
                    "Accounting batch must contain at least one item"
            );
        }
    }
}
