package com.sixpay.payment.infrastructure.outbox;

import com.sixpay.common.messaging.model.IntegrationEventEnvelope;
import com.sixpay.common.messaging.model.OutboxMessage;
import com.sixpay.common.messaging.outbox.OutboxMessageSource;
import com.sixpay.payment.infrastructure.outbox.claim.PaymentOutboxClaimService;
import com.sixpay.payment.infrastructure.outbox.claim.PaymentOutboxClaim;
import com.sixpay.payment.infrastructure.audit.PaymentAuditAdapter;
import com.sixpay.payment.infrastructure.audit.PaymentAuditEntry;
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
    private final PaymentAuditAdapter audit;

    public PaymentOutboxMessageSource(PaymentOutboxClaimService claims,
                                     PaymentOutboxCompletionService completion,
                                     PaymentAuditAdapter audit) {
        this.claims = Objects.requireNonNull(claims);
        this.completion = Objects.requireNonNull(completion);
        this.audit = Objects.requireNonNull(audit);
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
                .stream().map(this::toMessage).toList();
    }

    private OutboxMessage toMessage(PaymentOutboxClaim claim) {
        // Outbox stores the stable contract type; audit stores the domain event name.
        // Use the immutable event ID; never infer the type from JSON or a payment status.
        String eventType = claim.eventType();
        if (PaymentDomainEventMapper.PAYMENT_DOMAIN_EVENT_TYPE.equals(eventType)) {
            eventType = audit.findByPaymentId(claim.aggregateId()).stream()
                    .filter(entry -> claim.eventId().equals(entry.eventId()))
                    .map(PaymentAuditEntry::eventType)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Missing Payment audit for outbox event " + claim.eventId()));
        }
        return new OutboxMessage(
                new IntegrationEventEnvelope(claim.eventId(), eventType,
                        claim.schemaVersion(), claim.aggregateType(), claim.aggregateId(),
                        claim.correlationId(), claim.occurredAt(), claim.payload()),
                claim.attempt());
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
