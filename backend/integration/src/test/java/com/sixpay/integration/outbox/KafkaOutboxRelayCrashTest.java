package com.sixpay.integration.outbox;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KafkaOutboxRelayCrashTest {

    @Test
    void failedKafkaPublicationLeavesRecordUnpublished() {
        OutboxEventStore store = mock(
                OutboxEventStore.class
        );

        KafkaTemplate<String, byte[]> template =
                mock(KafkaTemplate.class);

        OutboxEventRecord record =
                new OutboxEventRecord(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "payment.posted.v1",
                        "payment-1",
                        "payment-1",
                        "{}".getBytes(),
                        Instant.parse(
                                "2026-08-06T20:00:00Z"
                        ),
                        0
                );

        when(store.claimBatch(
                anyInt(),
                any(),
                anyString()
        )).thenReturn(List.of(record));

        when(template.send(any(
                org.apache.kafka.clients.producer
                        .ProducerRecord.class
        ))).thenThrow(
                new RuntimeException("Kafka unavailable")
        );

        KafkaOutboxRelay relay =
                new KafkaOutboxRelay(
                        store,
                        template,
                        ignored ->
                                "sixpay.payment.financial.v1",
                        new SimpleMeterRegistry(),
                        Clock.fixed(
                                Instant.parse(
                                        "2026-08-06T20:00:00Z"
                                ),
                                ZoneOffset.UTC
                        ),
                        10,
                        "relay-test"
                );

        assertEquals(0, relay.publishNextBatch());

        verify(store, never()).markPublished(
                any(),
                any()
        );
        verify(store).markFailed(
                eq(record.outboxId()),
                any(),
                eq("KAFKA_PUBLISH_FAILED")
        );
    }
}
