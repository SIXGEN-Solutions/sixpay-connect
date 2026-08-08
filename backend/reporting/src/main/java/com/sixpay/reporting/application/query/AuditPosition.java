package com.sixpay.reporting.application.query;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AuditPosition(
        Instant occurredAt,
        UUID id
) {
    public AuditPosition {
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt is required");
        id = Objects.requireNonNull(id, "id is required");
    }
}
