package com.sixpay.accounting.domain.model;

import java.util.Objects;
import java.util.UUID;

public record AccountingProviderItemResult(
        UUID paymentId,
        AccountingBatchItemStatus status,
        String providerItemReference,
        String rejectionCode
) {
    public AccountingProviderItemResult {
        paymentId = Objects.requireNonNull(
                paymentId,
                "paymentId"
        );
        status = Objects.requireNonNull(
                status,
                "status"
        );
        providerItemReference = optional(
                providerItemReference
        );
        rejectionCode = optional(rejectionCode);
    }

    private static String optional(String value) {
        return value == null || value.isBlank()
                ? null
                : value.strip();
    }
}
