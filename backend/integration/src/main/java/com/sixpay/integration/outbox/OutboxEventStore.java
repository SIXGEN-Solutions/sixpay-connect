package com.sixpay.integration.outbox;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventStore {

    List<OutboxEventRecord> claimBatch(
            int batchSize,
            Instant claimedAt,
            String owner
    );

    void markPublished(
            UUID outboxId,
            Instant publishedAt
    );

    void markFailed(
            UUID outboxId,
            Instant failedAt,
            String safeErrorCode
    );

    int deletePublishedBefore(
            Instant cutoff,
            int batchSize
    );
}
