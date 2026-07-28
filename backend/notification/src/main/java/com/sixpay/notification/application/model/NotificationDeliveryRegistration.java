package com.sixpay.notification.application.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Transport-neutral data required to register the first delivery attempt.
 */
public record NotificationDeliveryRegistration(
        UUID eventId,
        UUID aggregateId,
        String eventType,
        String recipient,
        String template,
        String correlationId,
        Instant createdAt
) {

    public NotificationDeliveryRegistration {
        Objects.requireNonNull(eventId, "eventId is required");
        Objects.requireNonNull(aggregateId, "aggregateId is required");
        eventType = requireText(eventType, "eventType");
        recipient = requireText(recipient, "recipient");
        template = requireText(template, "template");
        correlationId = requireText(correlationId, "correlationId");
        Objects.requireNonNull(createdAt, "createdAt is required");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.strip();
    }
}
