package com.sixpay.partner.infrastructure.outbox;

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
        PUBLISHED,
        FAILED
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

    protected OutboxEventJpaEntity() {
    }

    public OutboxEventJpaEntity(
            UUID eventId,
            UUID aggregateId,
            String eventType,
            String payload,
            Instant occurredAt,
            Instant createdAt
    ) {
        this.eventId = eventId;
        this.aggregateType = "PARTNER";
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = Status.PENDING;
        this.occurredAt = occurredAt;
        this.createdAt = createdAt;
    }
}
