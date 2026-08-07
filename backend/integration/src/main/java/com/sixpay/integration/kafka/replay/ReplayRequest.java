package com.sixpay.integration.kafka.replay;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ReplayRequest(
        UUID replayId,
        UUID eventId,
        String sourceTopic,
        int sourcePartition,
        long sourceOffset,
        String requestedBy,
        String reason,
        Instant requestedAt
) {
    public ReplayRequest {
        replayId = Objects.requireNonNull(replayId);
        eventId = Objects.requireNonNull(eventId);
        sourceTopic = required(
                sourceTopic,
                "sourceTopic"
        );
        if (sourcePartition < 0) {
            throw new IllegalArgumentException(
                    "sourcePartition must be >= 0"
            );
        }
        if (sourceOffset < 0) {
            throw new IllegalArgumentException(
                    "sourceOffset must be >= 0"
            );
        }
        requestedBy = required(
                requestedBy,
                "requestedBy"
        );
        reason = required(reason, "reason");
        requestedAt = Objects.requireNonNull(
                requestedAt
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
