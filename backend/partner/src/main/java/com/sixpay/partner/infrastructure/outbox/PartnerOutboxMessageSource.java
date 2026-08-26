package com.sixpay.partner.infrastructure.outbox;

import com.sixpay.common.messaging.model.OutboxMessage;
import com.sixpay.common.messaging.outbox.OutboxMessageSource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class PartnerOutboxMessageSource implements OutboxMessageSource {

    private static final String SOURCE_NAME = "partner";

    private final OutboxEventSpringDataRepository repository;
    private final String claimantId = "partner-" + UUID.randomUUID();

    public PartnerOutboxMessageSource(OutboxEventSpringDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public String sourceName() {
        return SOURCE_NAME;
    }

    @Override
    @Transactional
    public List<OutboxMessage> claimPending(
            int batchSize,
            Instant now,
            Duration processingTimeout
    ) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("Batch size must be positive");
        }
        if (now == null) {
            throw new IllegalArgumentException("Current time must not be null");
        }
        if (processingTimeout == null || processingTimeout.isNegative()
                || processingTimeout.isZero()) {
            throw new IllegalArgumentException("Processing timeout must be positive");
        }

        Instant staleBefore = now.minus(processingTimeout);
        return repository.lockClaimable(now, staleBefore, batchSize).stream()
                .peek(message -> message.claim(now, claimantId))
                .map(OutboxEventJpaEntity::toOutboxMessage)
                .toList();
    }

    @Override
    @Transactional
    public void markPublished(UUID eventId, Instant publishedAt) {
        find(eventId).markPublished(publishedAt);
    }

    @Override
    @Transactional
    public void markFailed(
            UUID eventId,
            String reason,
            Instant failedAt,
            Instant nextAttemptAt
    ) {
        find(eventId).markFailed(reason, failedAt, nextAttemptAt);
    }

    @Override
    @Transactional
    public void markDead(UUID eventId, String reason, Instant failedAt) {
        find(eventId).markDead(reason, failedAt);
    }

    private OutboxEventJpaEntity find(UUID eventId) {
        return repository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException(
                        "Partner outbox event not found: " + eventId
                ));
    }
}
