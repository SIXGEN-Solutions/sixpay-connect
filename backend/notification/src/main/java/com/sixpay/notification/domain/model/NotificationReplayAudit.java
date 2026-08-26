package com.sixpay.notification.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record NotificationReplayAudit(
        UUID replayId,
        UUID notificationId,
        String operatorReference,
        String reason,
        NotificationDeliveryStatus previousStatus,
        Instant requestedAt
) {
    public NotificationReplayAudit {
        replayId = Objects.requireNonNull(
                replayId,
                "replayId"
        );
        notificationId = Objects.requireNonNull(
                notificationId,
                "notificationId"
        );
        operatorReference = required(
                operatorReference,
                "operatorReference",
                128
        );
        reason = required(
                reason,
                "reason",
                500
        );
        previousStatus = Objects.requireNonNull(
                previousStatus,
                "previousStatus"
        );
        requestedAt = Objects.requireNonNull(
                requestedAt,
                "requestedAt"
        );
    }

    private static String required(
            String value,
            String name,
            int maxLength
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    name + " is required"
            );
        }

        String normalized = value.strip();

        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    name + " exceeds " + maxLength + " characters"
            );
        }

        return normalized;
    }
}
