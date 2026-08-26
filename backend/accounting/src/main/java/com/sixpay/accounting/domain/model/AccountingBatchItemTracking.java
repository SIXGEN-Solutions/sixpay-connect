package com.sixpay.accounting.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AccountingBatchItemTracking(
        UUID paymentId,
        String providerItemReference,
        String rejectionCode,
        Instant updatedAt
) {
    public AccountingBatchItemTracking {
        paymentId = Objects.requireNonNull(
                paymentId,
                "paymentId"
        );
        providerItemReference = optional(
                providerItemReference
        );
        rejectionCode = optional(
                rejectionCode
        );
        updatedAt = Objects.requireNonNull(
                updatedAt,
                "updatedAt"
        );
    }

    private static String optional(String value) {
        return value == null || value.isBlank()
                ? null
                : value.strip();
    }
}
