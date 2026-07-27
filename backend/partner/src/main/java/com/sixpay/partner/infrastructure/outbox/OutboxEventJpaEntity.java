package com.sixpay.partner.infrastructure.outbox;

import com.sixpay.common.messaging.model.IntegrationEventEnvelope;
import com.sixpay.common.messaging.model.OutboxMessage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "partner_outbox_events")
public class OutboxEventJpaEntity {

    public enum Status {
        PENDING,
        PROCESSING,
        PUBLISHED,
        FAILED,
        DEAD
    }

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "aggregate_type", nullable = false, updatable = false, length = 64)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, updatable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, updatable = false, length = 150)
    private String eventType;

    @Column(name = "schema_version", nullable = false, updatable = false)
    private int schemaVersion;

    @Column(name = "correlation_id", nullable = false, updatable = false, length = 150)
    private String correlationId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, updatable = false, columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private Status status;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "claimed_at")
    private Instant claimedAt;

    @Column(name = "claimed_by", length = 100)
    private String claimedBy;

    protected OutboxEventJpaEntity() {
    }

    public OutboxEventJpaEntity(
            UUID eventId,
            UUID aggregateId,
            String eventType,
            int schemaVersion,
            String correlationId,
            String payload,
            Instant occurredAt,
            Instant createdAt
    ) {
        this.eventId = eventId;
        this.aggregateType = "PARTNER";
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.schemaVersion = schemaVersion;
        this.correlationId = correlationId;
        this.payload = payload;
        this.status = Status.PENDING;
        this.occurredAt = occurredAt;
        this.createdAt = createdAt;
        this.attemptCount = 0;
        this.nextAttemptAt = createdAt;
    }

    void claim(Instant claimedAt, String claimedBy) {
        this.status = Status.PROCESSING;
        this.attemptCount++;
        this.lastAttemptAt = claimedAt;
        this.claimedAt = claimedAt;
        this.claimedBy = claimedBy;
    }

    void markPublished(Instant publishedAt) {
        this.status = Status.PUBLISHED;
        this.publishedAt = publishedAt;
        this.failureReason = null;
        this.nextAttemptAt = publishedAt;
        clearClaim();
    }

    void markFailed(String failureReason, Instant failedAt, Instant nextAttemptAt) {
        this.status = Status.FAILED;
        this.failureReason = normalizeFailureReason(failureReason);
        this.lastAttemptAt = failedAt;
        this.nextAttemptAt = nextAttemptAt;
        clearClaim();
    }

    void markDead(String failureReason, Instant failedAt) {
        this.status = Status.DEAD;
        this.failureReason = normalizeFailureReason(failureReason);
        this.lastAttemptAt = failedAt;
        this.nextAttemptAt = failedAt;
        clearClaim();
    }

    OutboxMessage toOutboxMessage() {
        return new OutboxMessage(
                new IntegrationEventEnvelope(
                        eventId,
                        eventType,
                        schemaVersion,
                        aggregateType,
                        aggregateId,
                        correlationId,
                        occurredAt,
                        payload
                ),
                attemptCount
        );
    }

    private void clearClaim() {
        this.claimedAt = null;
        this.claimedBy = null;
    }

    private static String normalizeFailureReason(String failureReason) {
        if (failureReason == null || failureReason.isBlank()) {
            return "transport publication failed";
        }
        return failureReason.length() <= 1000
                ? failureReason
                : failureReason.substring(0, 1000);
    }
}
