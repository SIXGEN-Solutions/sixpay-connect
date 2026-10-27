#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path.cwd()
REL = Path("backend/payment/src/main/java/com/sixpay/payment/infrastructure/outbox/PaymentOutboxMessageSource.java")
JAVA = '''package com.sixpay.payment.infrastructure.outbox;

import com.sixpay.common.messaging.model.IntegrationEventEnvelope;
import com.sixpay.common.messaging.model.OutboxMessage;
import com.sixpay.common.messaging.outbox.OutboxMessageSource;
import com.sixpay.payment.infrastructure.audit.PaymentAuditAdapter;
import com.sixpay.payment.infrastructure.audit.PaymentAuditEntry;
import com.sixpay.payment.infrastructure.callback.PaymentCallbackProperties;
import com.sixpay.payment.infrastructure.outbox.claim.PaymentOutboxClaim;
import com.sixpay.payment.infrastructure.outbox.claim.PaymentOutboxClaimService;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Exposes Payment-owned outbox events to the generic relay.
 * The stored event_type is the canonical "payment.domain-event";
 * workflow dispatch needs the logical type recorded in the Payment audit.
 */
@Repository
public final class PaymentOutboxMessageSource implements OutboxMessageSource {

    private final String owner = "payment-relay-" + UUID.randomUUID();
    private final PaymentOutboxClaimService claims;
    private final PaymentOutboxCompletionService completion;
    private final PaymentAuditAdapter audit;

    public PaymentOutboxMessageSource(
            PaymentOutboxClaimService claims,
            PaymentOutboxCompletionService completion,
            PaymentAuditAdapter audit,
            PaymentCallbackProperties callbackProperties
    ) {
        this.claims = Objects.requireNonNull(claims);
        this.completion = Objects.requireNonNull(completion);
        this.audit = Objects.requireNonNull(audit);
        // Two independent consumers cannot ACK the same outbox row.
        // Fail fast instead of silently dropping workflow or callbacks.
        if (Objects.requireNonNull(callbackProperties).isEnabled()) {
            throw new IllegalStateException(
                    "Payment generic relay and legacy callback relay both claim "
                    + "payment_outbox_events. Disable sixpay.payment.callback.enabled "
                    + "until independent callback delivery tracking is approved.");
        }
    }

    @Override
    public String sourceName() {
        return "payment";
    }

    @Override
    public List<OutboxMessage> claimPending(
            int batchSize, Instant now, Duration processingTimeout
    ) {
        Objects.requireNonNull(now);
        Objects.requireNonNull(processingTimeout);
        if (processingTimeout.isZero() || processingTimeout.isNegative()) {
            throw new IllegalArgumentException("processingTimeout must be positive");
        }
        return claims.claimAvailable(
                now, now.minus(processingTimeout), batchSize, owner
        ).stream().map(this::map).toList();
    }

    private OutboxMessage map(PaymentOutboxClaim claim) {
        String logicalType = audit.findByPaymentId(claim.aggregateId()).stream()
                .filter(entry -> entry.eventId().equals(claim.eventId()))
                .map(PaymentAuditEntry::eventType)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Payment outbox event has no audit record: " + claim.eventId()));
        return new OutboxMessage(
                new IntegrationEventEnvelope(
                        claim.eventId(), logicalType, claim.schemaVersion(),
                        claim.aggregateType(), claim.aggregateId(),
                        claim.correlationId(), claim.occurredAt(), claim.payload()),
                claim.attempt());
    }

    @Override
    public void markPublished(UUID eventId, Instant publishedAt) {
        completion.markPublished(eventId, owner, publishedAt);
    }

    @Override
    public void markFailed(
            UUID eventId, String reason, Instant failedAt, Instant nextAttemptAt
    ) {
        completion.markRetryableFailure(
                eventId, owner, reason, failedAt, nextAttemptAt);
    }

    @Override
    public void markDead(UUID eventId, String reason, Instant failedAt) {
        completion.markDead(eventId, owner, reason, failedAt);
    }
}
'''
def main():
    if not (ROOT / "AGENTS.md").is_file():
        print("ERROR: lancer depuis la racine du repository.", file=sys.stderr)
        return 2
    dest = ROOT / REL
    if dest.exists():
        print("ERROR: fichier existant, aucun écrasement :", dest, file=sys.stderr)
        return 3
    dest.parent.mkdir(parents=True, exist_ok=True)
    with dest.open("x", encoding="utf-8", newline="\n") as f:
        f.write(JAVA)
    print("CREATED:", REL)
    print("Aucun gate, commit, push, worktree check exécuté.")
    return 0
if __name__ == "__main__":
    sys.exit(main())
