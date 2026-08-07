package com.sixpay.notification.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record AccountingBatchCompletedNotificationTrigger(
        UUID batchId,
        LocalDate businessDate,
        String financialInstitutionCode,
        int itemCount,
        Instant completedAt,
        String correlationId
) implements OperationalNotificationTrigger {

    public AccountingBatchCompletedNotificationTrigger {
        batchId = Objects.requireNonNull(
                batchId,
                "batchId"
        );

        businessDate = Objects.requireNonNull(
                businessDate,
                "businessDate"
        );

        if (financialInstitutionCode == null
                || financialInstitutionCode.isBlank()) {
            throw new IllegalArgumentException(
                    "financialInstitutionCode is required"
            );
        }

        financialInstitutionCode =
                financialInstitutionCode.strip();

        if (itemCount <= 0) {
            throw new IllegalArgumentException(
                    "itemCount must be positive"
            );
        }

        completedAt = Objects.requireNonNull(
                completedAt,
                "completedAt"
        );

        if (correlationId == null
                || correlationId.isBlank()) {
            throw new IllegalArgumentException(
                    "correlationId is required"
            );
        }

        correlationId = correlationId.strip();
    }

    @Override
    public OperationalNotificationTriggerType type() {
        return OperationalNotificationTriggerType
                .ACCOUNTING_BATCH_COMPLETED;
    }

    @Override
    public NotificationSourceReference
    sourceReference() {
        return new NotificationSourceReference(
                type(),
                batchId.toString()
        );
    }

    @Override
    public Instant occurredAt() {
        return completedAt;
    }
}
