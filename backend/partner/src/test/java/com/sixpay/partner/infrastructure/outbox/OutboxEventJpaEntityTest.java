package com.sixpay.partner.infrastructure.outbox;

import com.sixpay.common.messaging.model.OutboxMessage;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OutboxEventJpaEntityTest {

    @Test
    void exposesAClaimedEventAsATransportNeutralMessage() {
        UUID eventId = UUID.randomUUID();
        UUID partnerId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-07-27T10:00:00Z");
        OutboxEventJpaEntity entity = new OutboxEventJpaEntity(
                eventId,
                partnerId,
                "PartnerCreatedIntegrationEvent",
                1,
                "correlation-1",
                "{\"partnerId\":\"" + partnerId + "\"}",
                occurredAt,
                occurredAt
        );

        entity.claim(occurredAt, "test-relay");
        OutboxMessage message = entity.toOutboxMessage();

        assertEquals(eventId, message.event().eventId());
        assertEquals(partnerId, message.event().aggregateId());
        assertEquals("PARTNER", message.event().aggregateType());
        assertEquals("correlation-1", message.event().correlationId());
        assertEquals(1, message.event().schemaVersion());
        assertEquals(1, message.attemptCount());
    }
}
