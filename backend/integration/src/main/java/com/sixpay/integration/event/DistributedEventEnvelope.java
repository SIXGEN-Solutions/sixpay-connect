package com.sixpay.integration.event;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record DistributedEventEnvelope<T>(
        UUID eventId,
        String eventType,
        int schemaVersion,
        Instant occurredAt,
        String producer,
        String aggregateType,
        String aggregateId,
        String correlationId,
        String causationId,
        String partitionKey,
        PayloadClassification payloadClassification,
        T payload,
        Map<String, String> metadata
) {
    public DistributedEventEnvelope {
        eventId = Objects.requireNonNull(eventId, "eventId");
        eventType = required(eventType, "eventType");
        if (schemaVersion < 1) {
            throw new IllegalArgumentException(
                    "schemaVersion must be >= 1"
            );
        }
        occurredAt = Objects.requireNonNull(
                occurredAt,
                "occurredAt"
        );
        producer = required(producer, "producer");
        aggregateType = required(
                aggregateType,
                "aggregateType"
        );
        aggregateId = required(
                aggregateId,
                "aggregateId"
        );
        correlationId = required(
                correlationId,
                "correlationId"
        );
        causationId = nullable(causationId);
        partitionKey = required(
                partitionKey,
                "partitionKey"
        );
        payloadClassification = Objects.requireNonNull(
                payloadClassification,
                "payloadClassification"
        );
        payload = Objects.requireNonNull(payload, "payload");
        metadata = metadata == null
                ? Map.of()
                : Map.copyOf(metadata);
    }

    private static String required(
            String value,
            String name
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    name + " is required"
            );
        }
        return value.strip();
    }

    private static String nullable(String value) {
        return value == null || value.isBlank()
                ? null
                : value.strip();
    }
}
