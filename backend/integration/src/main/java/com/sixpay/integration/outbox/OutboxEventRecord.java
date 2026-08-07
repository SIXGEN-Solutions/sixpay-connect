package com.sixpay.integration.outbox;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record OutboxEventRecord(
        UUID outboxId,
        UUID eventId,
        String eventType,
        String aggregateId,
        String partitionKey,
        byte[] payload,
        Instant createdAt,
        int publicationAttempts
) {
    public OutboxEventRecord {
        outboxId = Objects.requireNonNull(outboxId);
        eventId = Objects.requireNonNull(eventId);
        eventType = required(eventType, "eventType");
        aggregateId = required(
                aggregateId,
                "aggregateId"
        );
        partitionKey = required(
                partitionKey,
                "partitionKey"
        );
        payload = Objects.requireNonNull(payload).clone();
        createdAt = Objects.requireNonNull(createdAt);
        if (publicationAttempts < 0) {
            throw new IllegalArgumentException(
                    "publicationAttempts must be >= 0"
            );
        }
    }

    @Override
    public byte[] payload() {
        return payload.clone();
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
}
