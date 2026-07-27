package com.sixpay.integration.messaging.outbox;

import com.sixpay.common.messaging.model.IntegrationEventEnvelope;
import com.sixpay.common.messaging.model.OutboxMessage;
import com.sixpay.common.messaging.outbox.OutboxMessageSource;
import com.sixpay.integration.messaging.properties.OutboxRelayProperties;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OutboxRelayTest {

    private static final Instant NOW =
            Instant.parse("2026-07-27T10:00:00Z");

    @Test
    void marksTheMessagePublishedAfterTransportConfirmation() {
        FakeSource source = new FakeSource(message(1));
        OutboxRelay relay = relay(source, event -> {
        });

        relay.poll();

        assertEquals(source.message.event().eventId(), source.publishedId);
        assertNull(source.failedId);
    }

    @Test
    void schedulesARetryWhenPublicationFails() {
        FakeSource source = new FakeSource(message(2));
        OutboxRelay relay = relay(source, event -> {
            throw new IllegalStateException("broker unavailable");
        });

        relay.poll();

        assertEquals(source.message.event().eventId(), source.failedId);
        assertEquals(NOW.plusSeconds(60), source.nextAttemptAt);
        assertNull(source.deadId);
    }

    @Test
    void marksTheMessageDeadAtTheMaximumAttempt() {
        FakeSource source = new FakeSource(message(5));
        OutboxRelay relay = relay(source, event -> {
            throw new IllegalStateException("broker unavailable");
        });

        relay.poll();

        assertEquals(source.message.event().eventId(), source.deadId);
        assertNull(source.failedId);
    }

    private static OutboxRelay relay(
            FakeSource source,
            com.sixpay.common.messaging.transport.IntegrationEventTransport transport
    ) {
        return new OutboxRelay(
                List.of(source),
                transport,
                new OutboxRelayProperties(
                        50,
                        5,
                        Duration.ofSeconds(30),
                        Duration.ofMinutes(5)
                ),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private static OutboxMessage message(int attemptCount) {
        return new OutboxMessage(
                new IntegrationEventEnvelope(
                        UUID.randomUUID(),
                        "PartnerCreatedIntegrationEvent",
                        1,
                        "PARTNER",
                        UUID.randomUUID(),
                        "correlation-1",
                        NOW,
                        "{}"
                ),
                attemptCount
        );
    }

    private static final class FakeSource implements OutboxMessageSource {

        private final OutboxMessage message;
        private UUID publishedId;
        private UUID failedId;
        private UUID deadId;
        private Instant nextAttemptAt;

        private FakeSource(OutboxMessage message) {
            this.message = message;
        }

        @Override
        public String sourceName() {
            return "test";
        }

        @Override
        public List<OutboxMessage> claimPending(
                int batchSize,
                Instant now,
                Duration processingTimeout
        ) {
            return List.of(message);
        }

        @Override
        public void markPublished(UUID eventId, Instant publishedAt) {
            this.publishedId = eventId;
        }

        @Override
        public void markFailed(
                UUID eventId,
                String reason,
                Instant failedAt,
                Instant nextAttemptAt
        ) {
            this.failedId = eventId;
            this.nextAttemptAt = nextAttemptAt;
        }

        @Override
        public void markDead(
                UUID eventId,
                String reason,
                Instant failedAt
        ) {
            this.deadId = eventId;
        }
    }
}
