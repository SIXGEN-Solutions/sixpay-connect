package com.sixpay.notification.infrastructure.messaging.kafka;

import com.sixpay.common.messaging.model.IntegrationEventEnvelope;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KafkaIntegrationEventListenerTest {

    @Test
    void deserializesAndDelegatesTheSameEnvelopeContract()
            throws Exception {
        AtomicReference<IntegrationEventEnvelope> handled =
                new AtomicReference<>();
        ObjectMapper objectMapper = new ObjectMapper();
        KafkaIntegrationEventListener listener =
                new KafkaIntegrationEventListener(
                        handled::set,
                        objectMapper
                );
        IntegrationEventEnvelope event = event();

        listener.onIntegrationEvent(
                objectMapper.writeValueAsString(event)
        );

        assertEquals(event, handled.get());
    }

    private static IntegrationEventEnvelope event() {
        return new IntegrationEventEnvelope(
                UUID.randomUUID(),
                "PartnerStatusChangedIntegrationEvent",
                1,
                "PARTNER",
                UUID.randomUUID(),
                "correlation-1",
                Instant.parse("2026-07-27T10:00:00Z"),
                "{\"currentStatus\":\"ACTIVE\"}"
        );
    }
}
