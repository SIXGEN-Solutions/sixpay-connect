package com.sixpay.integration.messaging.idempotency;
import java.time.Instant;
import java.util.UUID;
public interface ProcessedMessageStore {
    boolean tryClaim(UUID messageId, Instant claimedAt);
    void markProcessed(UUID messageId, Instant processedAt);
    void releaseClaim(UUID messageId);
}
