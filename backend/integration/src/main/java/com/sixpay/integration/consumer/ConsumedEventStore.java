package com.sixpay.integration.consumer;

import java.time.Instant;
import java.util.UUID;

public interface ConsumedEventStore {

    boolean tryStart(
            String consumerName,
            UUID eventId,
            Instant startedAt
    );

    void markCompleted(
            String consumerName,
            UUID eventId,
            Instant completedAt
    );

    void markFailed(
            String consumerName,
            UUID eventId,
            Instant failedAt,
            String safeErrorCode
    );
}
