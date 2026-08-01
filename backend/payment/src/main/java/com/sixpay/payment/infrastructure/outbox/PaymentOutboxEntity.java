package com.sixpay.payment.infrastructure.outbox;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "payment_outbox_events",
        indexes = {
                @Index(name = "idx_payment_outbox_claimable",
                        columnList = "status, next_attempt_at, occurred_at"),
                @Index(name = "idx_payment_outbox_aggregate",
                        columnList = "aggregate_id, occurred_at"),
                @Index(name = "idx_payment_outbox_correlation",
                        columnList = "correlation_id")
        }
)
public class PaymentOutboxEntity {

    public enum Status {
        PENDING, PROCESSING, PUBLISHED, FAILED, DEAD
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

    protected PaymentOutboxEntity() {
    }

    PaymentOutboxEntity(
            UUID eventId,
            UUID aggregateId,
            String eventType,
            int schemaVersion,
            String correlationId,
            String payload,
            Instant occurredAt,
            Instant createdAt
    ) {
        this.eventId = Objects.requireNonNull(eventId, "Outbox event ID");
        this.aggregateType = "PAYMENT";
        this.aggregateId = Objects.requireNonNull(aggregateId, "Outbox aggregate ID");
        this.eventType = requireText(eventType, 150, "Outbox event type");
        if (schemaVersion <= 0) {
            throw new IllegalArgumentException("Outbox schema version must be positive");
        }
        this.schemaVersion = schemaVersion;
        this.correlationId = requireText(correlationId, 150, "Outbox correlation ID");
        this.payload = requireText(payload, Integer.MAX_VALUE, "Outbox payload");
        this.occurredAt = Objects.requireNonNull(occurredAt, "Outbox occurrence instant");
        this.createdAt = Objects.requireNonNull(createdAt, "Outbox creation instant");
        if (createdAt.isBefore(occurredAt)) {
            throw new IllegalArgumentException(
                    "Outbox creation instant must not precede occurrence"
            );
        }
        status = Status.PENDING;
        attemptCount = 0;
        nextAttemptAt = createdAt;
    }

    void claim(Instant at, String owner) {
        status = Status.PROCESSING;
        attemptCount++;
        lastAttemptAt = Objects.requireNonNull(at, "Claim instant");
        claimedAt = at;
        claimedBy = requireText(owner, 100, "Claim owner");
    }

    void markPublished(Instant at) {
        status = Status.PUBLISHED;
        publishedAt = Objects.requireNonNull(at, "Publication instant");
        failureReason = null;
        nextAttemptAt = at;
        clearClaim();
    }

    void markFailed(String reason, Instant failedAt, Instant retryAt) {
        status = Status.FAILED;
        failureReason = normalizeFailureReason(reason);
        lastAttemptAt = Objects.requireNonNull(failedAt, "Failure instant");
        nextAttemptAt = Objects.requireNonNull(retryAt, "Next-attempt instant");
        clearClaim();
    }

    void markDead(String reason, Instant failedAt) {
        status = Status.DEAD;
        failureReason = normalizeFailureReason(reason);
        lastAttemptAt = Objects.requireNonNull(failedAt, "Dead-letter instant");
        nextAttemptAt = failedAt;
        clearClaim();
    }

    private void clearClaim() {
        claimedAt = null;
        claimedBy = null;
    }

    private static String normalizeFailureReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "transport publication failed";
        }
        return reason.length() <= 1000 ? reason : reason.substring(0, 1000);
    }

    private static String requireText(String value, int max, String label) {
        if (value == null || value.isBlank() || value.length() > max) {
            throw new IllegalArgumentException(
                    label + " must be non-blank and at most " + max + " characters"
            );
        }
        return value;
    }

    public UUID eventId() { return eventId; }
    public String aggregateType() { return aggregateType; }
    public UUID aggregateId() { return aggregateId; }
    public String eventType() { return eventType; }
    public int schemaVersion() { return schemaVersion; }
    public String correlationId() { return correlationId; }
    public String payload() { return payload; }
    public Status status() { return status; }
    public Instant occurredAt() { return occurredAt; }
    public Instant createdAt() { return createdAt; }
    public Instant publishedAt() { return publishedAt; }
    public String failureReason() { return failureReason; }
    public int attemptCount() { return attemptCount; }
    public Instant nextAttemptAt() { return nextAttemptAt; }
    public Instant lastAttemptAt() { return lastAttemptAt; }
    public Instant claimedAt() { return claimedAt; }
    public String claimedBy() { return claimedBy; }
}
