package com.sixpay.payment.infrastructure.outbox.serialization;

import com.sixpay.payment.application.event.projection
        .ObservedCustomerProjectionEventType;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Storage-neutral JSON envelope written to the Payment outbox.
 *
 * <p>The {@code eventType} field is a stable contract identifier and never a
 * Java class name. {@code projectionEventType} represents the logical Payment
 * event carried by the versioned projection contract.</p>
 */
public record PaymentOutboxEventEnvelope(
        UUID eventId,
        String eventType,
        int eventVersion,
        UUID aggregateId,
        String aggregateType,
        long aggregateVersion,
        ObservedCustomerProjectionEventType projectionEventType,
        String correlationId,
        Instant occurredAt,
        JsonNode payload
) {

    public PaymentOutboxEventEnvelope {
        eventId = Objects.requireNonNull(
                eventId,
                "eventId is required"
        );
        eventType = requireText(
                eventType,
                "eventType"
        );
        if (eventVersion < 1) {
            throw new IllegalArgumentException(
                    "eventVersion must be at least one"
            );
        }
        aggregateId = Objects.requireNonNull(
                aggregateId,
                "aggregateId is required"
        );
        aggregateType = requireText(
                aggregateType,
                "aggregateType"
        );
        if (aggregateVersion < 1) {
            throw new IllegalArgumentException(
                    "aggregateVersion must be at least one"
            );
        }
        projectionEventType = Objects.requireNonNull(
                projectionEventType,
                "projectionEventType is required"
        );
        correlationId = requireText(
                correlationId,
                "correlationId"
        );
        occurredAt = Objects.requireNonNull(
                occurredAt,
                "occurredAt is required"
        );
        payload = Objects.requireNonNull(
                payload,
                "payload is required"
        );
        if (!payload.isObject()) {
            throw new IllegalArgumentException(
                    "payload must be a JSON object"
            );
        }
    }

    private static String requireText(
            String value,
            String field
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " must not be blank"
            );
        }
        return value.strip();
    }

    @Override
    public String toString() {
        return "PaymentOutboxEventEnvelope["
                + "eventId="
                + eventId
                + ", eventType="
                + eventType
                + ", eventVersion="
                + eventVersion
                + ", aggregateId="
                + aggregateId
                + ", aggregateType="
                + aggregateType
                + ", aggregateVersion="
                + aggregateVersion
                + ", projectionEventType="
                + projectionEventType
                + ", correlationId="
                + correlationId
                + ", occurredAt="
                + occurredAt
                + ", payload=[PROTECTED]"
                + "]";
    }
}
