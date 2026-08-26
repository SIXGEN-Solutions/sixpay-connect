package com.sixpay.integration.messaging.internal;

import com.sixpay.common.messaging.model.IntegrationEventEnvelope;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertSame;

class InternalEventBusTransportTest {

    @Test
    void publishesTheEnvelopeOnTheSpringEventBus() {
        AtomicReference<Object> published = new AtomicReference<>();
        ApplicationEventPublisher publisher = published::set;
        InternalEventBusTransport transport =
                new InternalEventBusTransport(publisher);
        IntegrationEventEnvelope event = event();

        transport.publish(event);

        assertSame(event, published.get());
    }

    private static IntegrationEventEnvelope event() {
        return new IntegrationEventEnvelope(
                UUID.randomUUID(),
                "PartnerCreatedIntegrationEvent",
                1,
                "PARTNER",
                UUID.randomUUID(),
                "correlation-1",
                Instant.parse("2026-07-27T10:00:00Z"),
                "{}"
        );
    }
}
