package com.sixpay.common.messaging.outbox;

import com.sixpay.common.messaging.model.OutboxMessage;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Domain-owned Outbox exposed to the transport-neutral relay.
 *
 * <p>Implementations must atomically claim messages so that concurrent
 * application instances cannot publish the same row simultaneously.</p>
 */
public interface OutboxMessageSource {

    /**
     * Stable technical source name used for diagnostics.
     *
     * @return source name
     */
    String sourceName();

    /**
     * Claims a bounded batch of messages eligible for publication.
     *
     * @param batchSize maximum number of rows to claim
     * @param now current instant
     * @param processingTimeout duration after which an interrupted claim
     *                          may be recovered
     * @return claimed messages
     */
    List<OutboxMessage> claimPending(
            int batchSize,
            Instant now,
            Duration processingTimeout
    );

    /**
     * Marks a message as successfully published.
     *
     * @param eventId event identifier
     * @param publishedAt publication confirmation time
     */
    void markPublished(UUID eventId, Instant publishedAt);

    /**
     * Records a retryable publication failure.
     *
     * @param eventId event identifier
     * @param failureReason sanitized failure reason
     * @param failedAt failure time
     * @param nextAttemptAt next eligible publication time
     */
    void markFailed(
            UUID eventId,
            String failureReason,
            Instant failedAt,
            Instant nextAttemptAt
    );

    /**
     * Marks a message as permanently failed.
     *
     * @param eventId event identifier
     * @param failureReason sanitized failure reason
     * @param failedAt terminal failure time
     */
    void markDead(
            UUID eventId,
            String failureReason,
            Instant failedAt
    );
}
