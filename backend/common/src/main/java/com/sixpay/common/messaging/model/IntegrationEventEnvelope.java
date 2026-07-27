package com.sixpay.common.messaging.model;

import com.sixpay.common.validation.Preconditions;

import java.time.Instant;
import java.util.UUID;

/**
 * Transport-neutral representation of an integration event.
 *
 * @param eventId unique event identifier
 * @param eventType stable event type
 * @param schemaVersion event contract version
 * @param aggregateType source aggregate type
 * @param aggregateId source aggregate identifier
 * @param correlationId end-to-end correlation identifier
 * @param occurredAt business occurrence time
 * @param payload serialized event payload
 */
public record IntegrationEventEnvelope(
        UUID eventId,
        String eventType,
        int schemaVersion,
        String aggregateType,
        UUID aggregateId,
        String correlationId,
        Instant occurredAt,
        String payload
) {

    public IntegrationEventEnvelope {
        eventId = Preconditions.requireNonNull(
                eventId,
                "Event ID must not be null"
        );
        eventType = Preconditions.requireNonBlank(
                eventType,
                "Event type must not be blank"
        );
        Preconditions.requireTrue(
                schemaVersion > 0,
                "Schema version must be positive"
        );
        aggregateType = Preconditions.requireNonBlank(
                aggregateType,
                "Aggregate type must not be blank"
        );
        aggregateId = Preconditions.requireNonNull(
                aggregateId,
                "Aggregate ID must not be null"
        );
        correlationId = Preconditions.requireNonBlank(
                correlationId,
                "Correlation ID must not be blank"
        );
        occurredAt = Preconditions.requireNonNull(
                occurredAt,
                "Occurrence time must not be null"
        );
        payload = Preconditions.requireNonBlank(
                payload,
                "Event payload must not be blank"
        );
    }
}
