package com.sixpay.accounting.api.response;

import com.sixpay.accounting.domain.model.AccountingBatch;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AccountingBatchDetailResponse(
        UUID batchId,
        String idempotencyKey,
        LocalDate businessDate,
        String financialInstitutionCode,
        String status,
        int itemCount,
        Instant createdAt,
        List<AccountingBatchItemResponse> items
) {
    public AccountingBatchDetailResponse {
        items = List.copyOf(items);
    }

    public static AccountingBatchDetailResponse from(AccountingBatch batch) {
        return new AccountingBatchDetailResponse(
                batch.batchId().value(),
                batch.idempotencyKey().value(),
                batch.businessDate(),
                batch.financialInstitutionCode(),
                batch.status().name(),
                batch.items().size(),
                batch.createdAt(),
                batch.items().stream()
                        .map(AccountingBatchItemResponse::from)
                        .toList()
        );
    }
}
