package com.sixpay.payment.infrastructure.outbox;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentIntegrationMapperTest {

    @Test
    void mapsOutboxEntityToTransportNeutralEnvelope() {
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        Instant occurredAt =
                Instant.parse("2026-08-01T16:00:00Z");

        PaymentOutboxEntity entity =
                PaymentOutboxEntity.create(
                        eventId,
                        aggregateId,
                        "PaymentReceived",
                        1,
                        "corr-001",
                        "{\"payment\":\"received\"}",
                        occurredAt,
                        occurredAt.plusSeconds(1)
                );

        var envelope =
                new PaymentIntegrationMapper()
                        .toEnvelope(entity);

        assertThat(envelope.eventId())
                .isEqualTo(eventId);

        assertThat(envelope.eventType())
                .isEqualTo("PaymentReceived");

        assertThat(envelope.schemaVersion())
                .isOne();

        assertThat(envelope.aggregateType())
                .isEqualTo("PAYMENT");

        assertThat(envelope.aggregateId())
                .isEqualTo(aggregateId);

        assertThat(envelope.correlationId())
                .isEqualTo("corr-001");

        assertThat(envelope.occurredAt())
                .isEqualTo(occurredAt);

        assertThat(envelope.payload())
                .isEqualTo(
                        "{\"payment\":\"received\"}"
                );
    }
}