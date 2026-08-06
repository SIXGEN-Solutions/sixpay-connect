package com.sixpay.integration.messaging.idempotency;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

public final class IdempotentConsumerExecutor {
    private final ProcessedMessageStore store;
    private final Clock clock;
    public IdempotentConsumerExecutor(ProcessedMessageStore store, Clock clock) {
        this.store = Objects.requireNonNull(store);
        this.clock = Objects.requireNonNull(clock);
    }
    public ConsumptionResult execute(UUID messageId, MessageHandler handler) {
        Objects.requireNonNull(messageId, "messageId is required");
        Objects.requireNonNull(handler, "handler is required");
        if (!store.tryClaim(messageId, clock.instant())) return ConsumptionResult.DUPLICATE;
        try {
            handler.handle();
            store.markProcessed(messageId, clock.instant());
            return ConsumptionResult.PROCESSED;
        } catch (RuntimeException e) {
            store.releaseClaim(messageId);
            throw e;
        }
    }
    public enum ConsumptionResult { PROCESSED, DUPLICATE }
    @FunctionalInterface public interface MessageHandler { void handle(); }
}
