package com.sixpay.notification.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record NotificationIntent(
        UUID notificationId,
        NotificationSourceReference source,
        NotificationRecipient recipient,
        NotificationChannel channel,
        NotificationTemplateKey templateKey,
        NotificationDeduplicationKey deduplicationKey,
        Map<String, String> templateVariables,
        NotificationDeliveryStatus status,
        Instant createdAt,
        String correlationId
) {
    public NotificationIntent {
        notificationId = Objects.requireNonNull(
                notificationId,
                "notificationId"
        );
        source = Objects.requireNonNull(
                source,
                "source"
        );
        recipient = Objects.requireNonNull(
                recipient,
                "recipient"
        );
        channel = Objects.requireNonNull(
                channel,
                "channel"
        );
        templateKey = Objects.requireNonNull(
                templateKey,
                "templateKey"
        );
        deduplicationKey = Objects.requireNonNull(
                deduplicationKey,
                "deduplicationKey"
        );
        templateVariables = Map.copyOf(
                Objects.requireNonNull(
                        templateVariables,
                        "templateVariables"
                )
        );
        status = Objects.requireNonNull(
                status,
                "status"
        );
        createdAt = Objects.requireNonNull(
                createdAt,
                "createdAt"
        );

        if (correlationId == null
                || correlationId.isBlank()) {
            throw new IllegalArgumentException(
                    "correlationId is required"
            );
        }

        correlationId = correlationId.strip();
    }
}
