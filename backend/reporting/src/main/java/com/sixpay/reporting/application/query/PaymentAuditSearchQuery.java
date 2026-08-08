package com.sixpay.reporting.application.query;

import com.sixpay.reporting.domain.model.AuditActorType;
import com.sixpay.reporting.domain.model.AuditResult;
import com.sixpay.reporting.domain.model.AuditSort;
import com.sixpay.reporting.domain.model.AuditSourceSystem;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PaymentAuditSearchQuery(
        UUID paymentId,
        String paymentReference,
        UUID observedCustomerId,
        String actorId,
        AuditActorType actorType,
        String action,
        AuditResult result,
        String reasonCode,
        UUID correlationId,
        AuditSourceSystem sourceSystem,
        Instant occurredFrom,
        Instant occurredTo,
        AuditSort sort,
        AuditCursor cursor,
        int size,
        Instant snapshotAt
) {
    public static final int DEFAULT_SIZE = 50;
    public static final int MAX_SIZE = 200;

    public PaymentAuditSearchQuery {
        paymentReference = text(paymentReference, 64, "paymentReference");
        actorId = text(actorId, 128, "actorId");
        action = text(action, 100, "action");
        reasonCode = text(reasonCode, 64, "reasonCode");
        occurredFrom = Objects.requireNonNull(
                occurredFrom,
                "occurredFrom is required"
        );
        occurredTo = Objects.requireNonNull(
                occurredTo,
                "occurredTo is required"
        );
        if (occurredFrom.isAfter(occurredTo)) {
            throw new IllegalArgumentException(
                    "occurredFrom must not be after occurredTo"
            );
        }
        sort = sort == null ? AuditSort.OCCURRED_AT_DESC : sort;
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException(
                    "size must be between 1 and " + MAX_SIZE
            );
        }
        if (cursor == null) {
            snapshotAt = Objects.requireNonNull(
                    snapshotAt,
                    "snapshotAt is required for the first page"
            );
        }
    }

    private static String text(
            String value,
            int maxLength,
            String field
    ) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.isEmpty() || normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    field + " must contain between 1 and "
                            + maxLength + " characters"
            );
        }
        return normalized;
    }

    @Override
    public String toString() {
        return "PaymentAuditSearchQuery[filters=PROTECTED]";
    }
}
