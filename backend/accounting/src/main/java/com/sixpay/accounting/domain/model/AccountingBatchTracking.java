package com.sixpay.accounting.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record AccountingBatchTracking(
        AccountingBatchId batchId,
        AccountingSubmissionState submissionState,
        String providerBatchReference,
        Instant lastSubmissionAttemptAt,
        Instant lastReconciliationAt,
        int reconciliationAttempts,
        String lastErrorCode,
        List<AccountingBatchItemTracking> items
) {
    public AccountingBatchTracking {
        batchId = Objects.requireNonNull(
                batchId,
                "batchId"
        );
        submissionState = Objects.requireNonNull(
                submissionState,
                "submissionState"
        );
        providerBatchReference = optional(
                providerBatchReference
        );
        lastErrorCode = optional(lastErrorCode);

        if (reconciliationAttempts < 0) {
            throw new IllegalArgumentException(
                    "reconciliationAttempts must be >= 0"
            );
        }

        items = List.copyOf(
                items == null
                        ? List.of()
                        : items
        );
    }

    public static AccountingBatchTracking ready(
            AccountingBatchId batchId
    ) {
        return new AccountingBatchTracking(
                batchId,
                AccountingSubmissionState.READY,
                null,
                null,
                null,
                0,
                null,
                List.of()
        );
    }

    public AccountingBatchTracking submissionAttempted(
            Instant at
    ) {
        return new AccountingBatchTracking(
                batchId,
                AccountingSubmissionState.SUBMITTING,
                providerBatchReference,
                Objects.requireNonNull(at, "at"),
                lastReconciliationAt,
                reconciliationAttempts,
                lastErrorCode,
                items
        );
    }

    public AccountingBatchTracking outcomeUnknown(
            Instant at,
            String errorCode
    ) {
        return new AccountingBatchTracking(
                batchId,
                AccountingSubmissionState.OUTCOME_UNKNOWN,
                providerBatchReference,
                Objects.requireNonNull(at, "at"),
                lastReconciliationAt,
                reconciliationAttempts,
                errorCode,
                items
        );
    }

    public AccountingBatchTracking rejected(
            Instant at,
            String errorCode
    ) {
        return new AccountingBatchTracking(
                batchId,
                AccountingSubmissionState.REJECTED,
                providerBatchReference,
                Objects.requireNonNull(at, "at"),
                lastReconciliationAt,
                reconciliationAttempts,
                errorCode,
                items
        );
    }

    public AccountingBatchTracking submissionResolved(
            AccountingSubmissionState state,
            String providerReference,
            Instant at,
            List<AccountingBatchItemTracking> itemTracking
    ) {
        return new AccountingBatchTracking(
                batchId,
                Objects.requireNonNull(state, "state"),
                providerReference,
                Objects.requireNonNull(at, "at"),
                lastReconciliationAt,
                reconciliationAttempts,
                null,
                itemTracking
        );
    }

    public AccountingBatchTracking reconciled(
            AccountingSubmissionState state,
            String providerReference,
            Instant at,
            List<AccountingBatchItemTracking> itemTracking
    ) {
        return new AccountingBatchTracking(
                batchId,
                Objects.requireNonNull(state, "state"),
                providerReference,
                lastSubmissionAttemptAt,
                Objects.requireNonNull(at, "at"),
                reconciliationAttempts + 1,
                null,
                itemTracking
        );
    }

    public AccountingBatchTracking reconciliationMiss(
            Instant at
    ) {
        return new AccountingBatchTracking(
                batchId,
                submissionState,
                providerBatchReference,
                lastSubmissionAttemptAt,
                Objects.requireNonNull(at, "at"),
                reconciliationAttempts + 1,
                lastErrorCode,
                items
        );
    }

    private static String optional(String value) {
        return value == null || value.isBlank()
                ? null
                : value.strip();
    }
}
