package com.sixpay.notification.application.model;

import java.util.Objects;
import java.util.UUID;

/**
 * A persisted delivery that has been atomically claimed for an attempt.
 */
public record NotificationDeliveryAttempt(
        UUID eventId,
        UUID aggregateId,
        String recipient,
        String template,
        String reason,
        String correlationId,
        int attemptCount
) {

    public NotificationDeliveryAttempt {
        Objects.requireNonNull(eventId, "eventId is required");
        Objects.requireNonNull(aggregateId, "aggregateId is required");
        recipient = requireText(recipient, "recipient");
        template = requireText(template, "template");
        correlationId = requireText(correlationId, "correlationId");
        if (attemptCount < 1) {
            throw new IllegalArgumentException(
                    "attemptCount must be greater than zero"
            );
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.strip();
    }
}
