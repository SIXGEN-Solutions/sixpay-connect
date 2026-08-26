package com.sixpay.customer.management.application.audit;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CustomerAuditRecord(
        UUID auditId,
        String aggregateType,
        UUID aggregateId,
        String action,
        String result,
        String actorId,
        String correlationId,
        String details,
        Instant occurredAt
) {
    public CustomerAuditRecord {
        Objects.requireNonNull(auditId, "auditId is required");
        aggregateType = requireText(aggregateType, "aggregateType");
        Objects.requireNonNull(aggregateId, "aggregateId is required");
        action = requireText(action, "action");
        result = requireText(result, "result");
        actorId = requireText(actorId, "actorId");
        correlationId = requireText(correlationId, "correlationId");
        details = requireText(details, "details");
        Objects.requireNonNull(occurredAt, "occurredAt is required");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.strip();
    }
}
