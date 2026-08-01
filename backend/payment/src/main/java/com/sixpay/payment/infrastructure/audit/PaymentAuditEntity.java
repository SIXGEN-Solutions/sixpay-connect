package com.sixpay.payment.infrastructure.audit;

import com.sixpay.payment.domain.event.PaymentDomainEvent;
import com.sixpay.payment.domain.model.PaymentStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "payment_audit",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_payment_audit_payment_version_sequence",
                columnNames = {"payment_id", "business_version", "event_sequence"}
        ),
        indexes = {
                @Index(name = "idx_payment_audit_payment_occurred_at", columnList = "payment_id, occurred_at"),
                @Index(name = "idx_payment_audit_correlation_id", columnList = "correlation_id"),
                @Index(name = "idx_payment_audit_event_type", columnList = "event_type")
        }
)
public class PaymentAuditEntity {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "payment_id", nullable = false, updatable = false)
    private UUID paymentId;

    @Column(name = "public_payment_reference", nullable = false, updatable = false, length = 30)
    private String publicPaymentReference;

    @Column(name = "event_type", nullable = false, updatable = false, length = 96)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, updatable = false, length = 48)
    private PaymentStatus paymentStatus;

    @Column(name = "business_version", nullable = false, updatable = false)
    private long businessVersion;

    @Column(name = "event_sequence", nullable = false, updatable = false)
    private int eventSequence;

    @Column(name = "correlation_id", nullable = false, updatable = false, length = 150)
    private String correlationId;

    @Column(name = "causation_id", updatable = false)
    private UUID causationId;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected PaymentAuditEntity() {}

    static PaymentAuditEntity from(PaymentDomainEvent event) {
        Objects.requireNonNull(event, "Payment domain event");
        PaymentAuditEntity entity = new PaymentAuditEntity();
        entity.eventId = event.eventId();
        entity.paymentId = event.paymentId().value();
        entity.publicPaymentReference = event.paymentReference().value();
        entity.eventType = event.getClass().getSimpleName();
        entity.paymentStatus = event.paymentStatus();
        entity.businessVersion = event.aggregateVersion();
        entity.eventSequence = event.eventSequence();
        entity.correlationId = event.correlationId().value();
        entity.causationId = event.causationId();
        entity.occurredAt = event.occurredAt();
        entity.validate();
        return entity;
    }

    private void validate() {
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(paymentId);
        Objects.requireNonNull(publicPaymentReference);
        Objects.requireNonNull(eventType);
        Objects.requireNonNull(paymentStatus);
        Objects.requireNonNull(correlationId);
        Objects.requireNonNull(occurredAt);
        if (eventType.isBlank() || eventType.length() > 96) {
            throw new IllegalArgumentException("Invalid audit event type");
        }
        if (businessVersion <= 0 || eventSequence <= 0) {
            throw new IllegalArgumentException("Invalid audit ordering metadata");
        }
    }

    UUID eventId() { return eventId; }
    UUID paymentId() { return paymentId; }
    String publicPaymentReference() { return publicPaymentReference; }
    String eventType() { return eventType; }
    PaymentStatus paymentStatus() { return paymentStatus; }
    long businessVersion() { return businessVersion; }
    int eventSequence() { return eventSequence; }
    String correlationId() { return correlationId; }
    UUID causationId() { return causationId; }
    Instant occurredAt() { return occurredAt; }
}
