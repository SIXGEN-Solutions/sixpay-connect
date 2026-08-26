package com.sixpay.integration.consumer;

import com.sixpay.integration.event.DistributedEventEnvelope;
import com.sixpay.integration.event.PayloadClassification;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IdempotentEventConsumerTest {

    @Test
    void processesSameEventOnlyOnceFunctionally() {
        UUID eventId = UUID.randomUUID();
        AtomicInteger handled = new AtomicInteger();

        ConsumedEventStore store =
                new InMemoryConsumedEventStore();

        IdempotentEventConsumer consumer =
                new IdempotentEventConsumer(
                        "test-consumer",
                        store,
                        Clock.fixed(
                                Instant.parse(
                                        "2026-08-06T20:00:00Z"
                                ),
                                ZoneOffset.UTC
                        ),
                        new SimpleMeterRegistry()
                );

        var event = new DistributedEventEnvelope<>(
                eventId,
                "payment.posted.v1",
                1,
                Instant.parse("2026-08-06T20:00:00Z"),
                "payment",
                "Payment",
                "payment-1",
                "corr-1",
                null,
                "payment-1",
                PayloadClassification.INTERNAL,
                Map.of(),
                Map.of()
        );

        consumer.consume(
                event,
                ignored -> handled.incrementAndGet()
        );
        consumer.consume(
                event,
                ignored -> handled.incrementAndGet()
        );

        assertEquals(1, handled.get());
    }

    private static final class InMemoryConsumedEventStore
            implements ConsumedEventStore {

        private final java.util.Set<String> keys =
                new java.util.HashSet<>();

        @Override
        public boolean tryStart(
                String consumerName,
                UUID eventId,
                Instant startedAt
        ) {
            return keys.add(
                    consumerName + ":" + eventId
            );
        }

        @Override
        public void markCompleted(
                String consumerName,
                UUID eventId,
                Instant completedAt
        ) {
        }

        @Override
        public void markFailed(
                String consumerName,
                UUID eventId,
                Instant failedAt,
                String safeErrorCode
        ) {
        }
    }
}
