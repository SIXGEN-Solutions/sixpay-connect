package com.sixpay.payment.infrastructure.outbox;

import com.sixpay.common.messaging.model.IntegrationEventEnvelope;
import com.sixpay.common.messaging.model.OutboxMessage;
import com.sixpay.common.messaging.outbox.OutboxMessageSource;
import com.sixpay.payment.infrastructure.outbox.claim.PaymentOutboxClaimService;
import org.springframework.stereotype.Repository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Payment-owned bridge to the provider-neutral outbox relay. */
@Repository
public class PaymentOutboxMessageSource implements OutboxMessageSource {
    private final String claimantId = "payment-relay-" + UUID.randomUUID();
    private final PaymentOutboxClaimService claims;
    private final PaymentOutboxCompletionService completion;

    public PaymentOutboxMessageSource(PaymentOutboxClaimService claims,
                                     PaymentOutboxCompletionService completion) {
        this.claims = Objects.requireNonNull(claims);
        this.completion = Objects.requireNonNull(completion);
    }

    @Override public String sourceName() { return "payment"; }

    @Override
    public List<OutboxMessage> claimPending(int batchSize, Instant now,
                                           Duration processingTimeout) {
        Objects.requireNonNull(now);
        Objects.requireNonNull(processingTimeout);
        if (processingTimeout.isZero() || processingTimeout.isNegative()) {
            throw new IllegalArgumentException("processingTimeout must be positive");
        }
        return claims.claimAvailable(now, now.minus(processingTimeout), batchSize, claimantId)
                .stream().map(c -> new OutboxMessage(
                        new IntegrationEventEnvelope(c.eventId(), c.eventType(),
                                c.schemaVersion(), c.aggregateType(), c.aggregateId(),
                                c.correlationId(), c.occurredAt(), c.payload()),
                        c.attempt())).toList();
    }

    @Override public void markPublished(UUID eventId, Instant at) {
        completion.markPublished(eventId, claimantId, at);
    }

    @Override
    public void markFailed(UUID eventId, String reason, Instant failedAt,
                           Instant nextAttemptAt) {
        completion.markRetryableFailure(eventId, claimantId, reason, failedAt, nextAttemptAt);
    }

    @Override public void markDead(UUID eventId, String reason, Instant failedAt) {
        completion.markDead(eventId, claimantId, reason, failedAt);
    }
}
