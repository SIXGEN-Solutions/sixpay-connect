package com.sixpay.integration.messaging.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Reliable Outbox relay settings.
 *
 * @param batchSize maximum messages claimed from each source per poll
 * @param maxAttempts maximum publication attempts before terminal failure
 * @param retryDelay base delay before retrying a failed publication
 * @param processingTimeout timeout used to recover interrupted claims
 */
@ConfigurationProperties(prefix = "sixpay.messaging.outbox")
public record OutboxRelayProperties(
        int batchSize,
        int maxAttempts,
        Duration retryDelay,
        Duration processingTimeout
) {

    public OutboxRelayProperties {
        if (batchSize == 0) {
            batchSize = 50;
        }
        if (maxAttempts == 0) {
            maxAttempts = 5;
        }
        if (retryDelay == null) {
            retryDelay = Duration.ofSeconds(30);
        }
        if (processingTimeout == null) {
            processingTimeout = Duration.ofMinutes(5);
        }
        if (batchSize < 1) {
            throw new IllegalArgumentException(
                    "Outbox batch size must be positive"
            );
        }
        if (maxAttempts < 1) {
            throw new IllegalArgumentException(
                    "Outbox max attempts must be positive"
            );
        }
        if (retryDelay.isNegative() || retryDelay.isZero()) {
            throw new IllegalArgumentException(
                    "Outbox retry delay must be positive"
            );
        }
        if (processingTimeout.isNegative()
                || processingTimeout.isZero()) {
            throw new IllegalArgumentException(
                    "Outbox processing timeout must be positive"
            );
        }
    }
}
