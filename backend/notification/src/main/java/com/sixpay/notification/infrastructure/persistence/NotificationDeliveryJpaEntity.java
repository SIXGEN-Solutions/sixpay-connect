package com.sixpay.notification.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_deliveries", schema = "sixpay")
public class NotificationDeliveryJpaEntity {

    @Id
    private UUID id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 150)
    private String eventType;

    @Column(nullable = false, length = 254)
    private String recipient;

    @Column(nullable = false, length = 100)
    private String template;

    @Column(length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private NotificationDeliveryStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "last_error", length = 2000)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "correlation_id", nullable = false, length = 150)
    private String correlationId;

    protected NotificationDeliveryJpaEntity() {
    }

    public UUID id() {
        return id;
    }

    public UUID eventId() {
        return eventId;
    }

    public UUID aggregateId() {
        return aggregateId;
    }

    public String recipient() {
        return recipient;
    }

    public String template() {
        return template;
    }

    public String reason() {
        return reason;
    }

    public String correlationId() {
        return correlationId;
    }

    public NotificationDeliveryStatus status() {
        return status;
    }

    public int attemptCount() {
        return attemptCount;
    }

    public Instant nextAttemptAt() {
        return nextAttemptAt;
    }

    public String lastError() {
        return lastError;
    }

    public Instant sentAt() {
        return sentAt;
    }

    void claimForRetry() {
        if (status != NotificationDeliveryStatus.FAILED
                && status != NotificationDeliveryStatus.PENDING) {
            throw new IllegalStateException(
                    "Only FAILED or PENDING delivery can be claimed"
            );
        }
        status = NotificationDeliveryStatus.PROCESSING;
        attemptCount++;
        nextAttemptAt = null;
    }
}
