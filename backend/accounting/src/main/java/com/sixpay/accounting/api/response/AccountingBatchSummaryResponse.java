package com.sixpay.accounting.api.response;

import com.sixpay.accounting.domain.model.AccountingBatch;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AccountingBatchSummaryResponse(
        UUID batchId,
        LocalDate businessDate,
        String financialInstitutionCode,
        String status,
        int itemCount,
        Instant createdAt
) {
    public static AccountingBatchSummaryResponse from(AccountingBatch batch) {
        return new AccountingBatchSummaryResponse(
                batch.batchId().value(),
                batch.businessDate(),
                batch.financialInstitutionCode(),
                batch.status().name(),
                batch.items().size(),
                batch.createdAt()
        );
    }
}
