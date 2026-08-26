package com.sixpay.integration.messaging.dlq;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record DeadLetterRecord(
        UUID messageId,
        String source,
        String destination,
        String errorCategory,
        String safeReason,
        int attempts,
        Instant failedAt,
        String correlationId,
        Map<String, String> metadata,
        String payload
) {
    public DeadLetterRecord {
        messageId = Objects.requireNonNull(messageId, "messageId is required");
        source = required(source, "source");
        destination = required(destination, "destination");
        errorCategory = required(errorCategory, "errorCategory");
        safeReason = required(safeReason, "safeReason");
        if (attempts < 1) throw new IllegalArgumentException("attempts must be positive");
        failedAt = Objects.requireNonNull(failedAt, "failedAt is required");
        correlationId = required(correlationId, "correlationId");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        payload = required(payload, "payload");
    }
    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        return value.strip();
    }
}
