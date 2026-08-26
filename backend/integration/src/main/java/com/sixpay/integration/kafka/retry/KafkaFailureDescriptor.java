package com.sixpay.integration.kafka.retry;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record KafkaFailureDescriptor(
        UUID eventId,
        String eventType,
        String consumerGroup,
        String originalTopic,
        int originalPartition,
        long originalOffset,
        int retryCount,
        KafkaFailureCategory category,
        String safeErrorCode,
        Instant firstFailureAt,
        Instant lastFailureAt
) {
    public KafkaFailureDescriptor {
        eventId = Objects.requireNonNull(eventId);
        eventType = required(eventType, "eventType");
        consumerGroup = required(
                consumerGroup,
                "consumerGroup"
        );
        originalTopic = required(
                originalTopic,
                "originalTopic"
        );
        if (originalPartition < 0) {
            throw new IllegalArgumentException(
                    "originalPartition must be >= 0"
            );
        }
        if (originalOffset < 0) {
            throw new IllegalArgumentException(
                    "originalOffset must be >= 0"
            );
        }
        if (retryCount < 0) {
            throw new IllegalArgumentException(
                    "retryCount must be >= 0"
            );
        }
        category = Objects.requireNonNull(category);
        safeErrorCode = required(
                safeErrorCode,
                "safeErrorCode"
        );
        firstFailureAt = Objects.requireNonNull(
                firstFailureAt
        );
        lastFailureAt = Objects.requireNonNull(
                lastFailureAt
        );
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
