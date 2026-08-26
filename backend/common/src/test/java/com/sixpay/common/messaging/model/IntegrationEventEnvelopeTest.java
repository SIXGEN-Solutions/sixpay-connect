package com.sixpay.common.messaging.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IntegrationEventEnvelopeTest {

    @Test
    void shouldCreateTransportNeutralEnvelope() {
        UUID eventId = UUID.fromString(
                "123e4567-e89b-12d3-a456-426614174000"
        );
        UUID aggregateId = UUID.fromString(
                "123e4567-e89b-12d3-a456-426614174001"
        );

        IntegrationEventEnvelope envelope =
                new IntegrationEventEnvelope(
                        eventId,
                        "PartnerCreatedIntegrationEvent",
                        1,
                        "PARTNER",
                        aggregateId,
                        "correlation-123",
                        Instant.parse("2026-07-27T12:00:00Z"),
                        "{\"partnerId\":\"" + aggregateId + "\"}"
                );

        assertEquals(eventId, envelope.eventId());
        assertEquals(1, envelope.schemaVersion());
        assertEquals("PARTNER", envelope.aggregateType());
    }

    @Test
    void shouldRejectInvalidSchemaVersion() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new IntegrationEventEnvelope(
                        UUID.randomUUID(),
                        "PartnerCreatedIntegrationEvent",
                        0,
                        "PARTNER",
                        UUID.randomUUID(),
                        "correlation-123",
                        Instant.parse("2026-07-27T12:00:00Z"),
                        "{}"
                )
        );
    }
}
