package com.sixpay.integration.messaging.idempotency;

import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.assertThat;

class IdempotentConsumerExecutorTest {
    @Test
    void processesMessageOnlyOnce() {
        InMemoryStore store = new InMemoryStore();
        IdempotentConsumerExecutor executor = new IdempotentConsumerExecutor(
                store,
                Clock.fixed(Instant.parse("2026-08-06T12:00:00Z"), ZoneOffset.UTC)
        );
        UUID id = UUID.randomUUID();
        AtomicInteger calls = new AtomicInteger();

        assertThat(executor.execute(id, calls::incrementAndGet))
                .isEqualTo(IdempotentConsumerExecutor.ConsumptionResult.PROCESSED);
        assertThat(executor.execute(id, calls::incrementAndGet))
                .isEqualTo(IdempotentConsumerExecutor.ConsumptionResult.DUPLICATE);
        assertThat(calls).hasValue(1);
    }

    private static final class InMemoryStore implements ProcessedMessageStore {
        private final Set<UUID> claimed = new HashSet<>();
        private final Set<UUID> processed = new HashSet<>();
        @Override public boolean tryClaim(UUID id, Instant at) {
            return !processed.contains(id) && claimed.add(id);
        }
        @Override public void markProcessed(UUID id, Instant at) {
            claimed.remove(id);
            processed.add(id);
        }
        @Override public void releaseClaim(UUID id) { claimed.remove(id); }
    }
}
