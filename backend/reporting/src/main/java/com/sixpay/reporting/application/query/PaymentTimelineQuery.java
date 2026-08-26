package com.sixpay.reporting.application.query;

import com.sixpay.reporting.domain.model.AuditEvidenceCategory;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PaymentTimelineQuery(
        UUID paymentId,
        AuditEvidenceCategory category,
        Instant occurredFrom,
        Instant occurredTo,
        AuditCursor cursor,
        int size,
        Instant snapshotAt
) {
    public static final int DEFAULT_SIZE = 50;
    public static final int MAX_SIZE = 200;

    public PaymentTimelineQuery {
        paymentId = Objects.requireNonNull(paymentId, "paymentId is required");
        if (occurredFrom != null && occurredTo != null
                && occurredFrom.isAfter(occurredTo)) {
            throw new IllegalArgumentException(
                    "occurredFrom must not be after occurredTo"
            );
        }
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
}
