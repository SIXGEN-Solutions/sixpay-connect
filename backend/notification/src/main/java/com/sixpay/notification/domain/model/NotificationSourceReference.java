package com.sixpay.notification.domain.model;

import java.util.Objects;

public record NotificationSourceReference(
        OperationalNotificationTriggerType triggerType,
        String sourceId
) {
    public NotificationSourceReference {
        triggerType = Objects.requireNonNull(
                triggerType,
                "triggerType"
        );

        if (sourceId == null || sourceId.isBlank()) {
            throw new IllegalArgumentException(
                    "sourceId is required"
            );
        }

        sourceId = sourceId.strip();
    }
}
