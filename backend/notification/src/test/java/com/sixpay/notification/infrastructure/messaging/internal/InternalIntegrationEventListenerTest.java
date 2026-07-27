package com.sixpay.notification.infrastructure.messaging.internal;

import com.sixpay.common.messaging.model.IntegrationEventEnvelope;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertSame;

class InternalIntegrationEventListenerTest {

    @Test
    void delegatesTheEnvelopeToTheApplicationPort() {
        AtomicReference<IntegrationEventEnvelope> handled =
                new AtomicReference<>();
        InternalIntegrationEventListener listener =
                new InternalIntegrationEventListener(handled::set);
        IntegrationEventEnvelope event = event();

        listener.onIntegrationEvent(event);

        assertSame(event, handled.get());
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
                "{}"
        );
    }
}
