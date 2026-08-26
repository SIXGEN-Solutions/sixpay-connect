package com.sixpay.accounting.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record AccountingProviderBatchResult(
        AccountingBatchId batchId,
        AccountingBatchIdempotencyKey idempotencyKey,
        AccountingBatchStatus status,
        String providerBatchReference,
        Instant processedAt,
        List<AccountingProviderItemResult> items
) {
    public AccountingProviderBatchResult {
        batchId = Objects.requireNonNull(
                batchId,
                "batchId"
        );
        idempotencyKey = Objects.requireNonNull(
                idempotencyKey,
                "idempotencyKey"
        );
        status = Objects.requireNonNull(
                status,
                "status"
        );
        providerBatchReference =
                providerBatchReference == null
                        || providerBatchReference.isBlank()
                        ? null
                        : providerBatchReference.strip();
        processedAt = Objects.requireNonNull(
                processedAt,
                "processedAt"
        );
        items = List.copyOf(
                Objects.requireNonNull(
                        items,
                        "items"
                )
        );
    }
}
