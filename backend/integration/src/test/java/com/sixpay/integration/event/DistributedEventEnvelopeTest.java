package com.sixpay.integration.event;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DistributedEventEnvelopeTest {

    @Test
    void requiresStableIdentityAndPartitionKey() {
        UUID eventId = UUID.randomUUID();

        var envelope = new DistributedEventEnvelope<>(
                eventId,
                "payment.posted.v1",
                1,
                Instant.parse("2026-08-06T20:00:00Z"),
                "payment",
                "Payment",
                "payment-1",
                "corr-1",
                "cause-1",
                "payment-1",
                PayloadClassification.INTERNAL,
                Map.of("status", "COMPLETED"),
                Map.of()
        );

        assertEquals(eventId, envelope.eventId());
        assertEquals(
                envelope.aggregateId(),
                envelope.partitionKey()
        );
    }

    @Test
    void rejectsUnsupportedSchemaVersion() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new DistributedEventEnvelope<>(
                        UUID.randomUUID(),
                        "payment.posted.v1",
                        0,
                        Instant.now(),
                        "payment",
                        "Payment",
                        "payment-1",
                        "corr-1",
                        null,
                        "payment-1",
                        PayloadClassification.INTERNAL,
                        Map.of(),
                        Map.of()
                )
        );
    }
}
