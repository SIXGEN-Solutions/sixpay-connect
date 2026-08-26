package com.sixpay.common.messaging.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OutboxMessageTest {

    @Test
    void shouldRequirePositiveAttemptCount() {
        IntegrationEventEnvelope envelope =
                new IntegrationEventEnvelope(
                        UUID.randomUUID(),
                        "PartnerCreatedIntegrationEvent",
                        1,
                        "PARTNER",
                        UUID.randomUUID(),
                        "correlation-123",
                        Instant.parse("2026-07-27T12:00:00Z"),
                        "{}"
                );

        OutboxMessage message = new OutboxMessage(envelope, 1);

        assertEquals(1, message.attemptCount());
        assertThrows(
                IllegalArgumentException.class,
                () -> new OutboxMessage(envelope, 0)
        );
    }
}
