package com.sixpay.customer.observation.infrastructure.audit.entity;

import com.sixpay.customer.observation.application.audit
        .ObservedCustomerAuditAction;
import com.sixpay.customer.observation.application.audit
        .ObservedCustomerAuditOutcome;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Immutable
@Table(
        name = "customer_observation_audit",
        indexes = {
                @Index(
                        name = "idx_customer_observation_audit_customer",
                        columnList = "observed_customer_id, occurred_at"
                ),
                @Index(
                        name = "idx_customer_observation_audit_source_event",
                        columnList = "source_event_id"
                ),
                @Index(
                        name = "idx_customer_observation_audit_correlation",
                        columnList = "correlation_id, occurred_at"
                ),
                @Index(
                        name = "idx_customer_observation_audit_occurred",
                        columnList = "occurred_at"
                )
        }
)
public class ObservedCustomerAuditJpaEntity {

    @Id
    @Column(name = "audit_id", nullable = false, updatable = false)
    private UUID auditId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, updatable = false, length = 64)
    private ObservedCustomerAuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, updatable = false, length = 32)
    private ObservedCustomerAuditOutcome outcome;

    @Column(name = "observed_customer_id", updatable = false)
    private UUID observedCustomerId;

    @Column(name = "payment_id", updatable = false)
    private UUID paymentId;

    @Column(name = "source_event_id", updatable = false)
    private UUID sourceEventId;

    @Column(name = "actor_id", nullable = false, updatable = false, length = 150)
    private String actorId;

    @Column(name = "correlation_id", nullable = false, updatable = false, length = 150)
    private String correlationId;

    @Column(name = "reason_code", updatable = false, length = 100)
    private String reasonCode;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "audit_version", nullable = false, updatable = false)
    private int auditVersion;

    protected ObservedCustomerAuditJpaEntity() {
        // Required by JPA.
    }

    private ObservedCustomerAuditJpaEntity(
            UUID auditId,
            ObservedCustomerAuditAction action,
            ObservedCustomerAuditOutcome outcome,
            UUID observedCustomerId,
            UUID paymentId,
            UUID sourceEventId,
            String actorId,
            String correlationId,
            String reasonCode,
            Instant occurredAt,
            int auditVersion
    ) {
        this.auditId = Objects.requireNonNull(auditId, "auditId is required");
        this.action = Objects.requireNonNull(action, "action is required");
        this.outcome = Objects.requireNonNull(outcome, "outcome is required");
        this.observedCustomerId = observedCustomerId;
        this.paymentId = paymentId;
        this.sourceEventId = sourceEventId;
        this.actorId = requireText(actorId, 150, "actorId");
        this.correlationId = requireText(correlationId, 150, "correlationId");
        this.reasonCode = normalizeReasonCode(reasonCode);
        this.occurredAt = Objects.requireNonNull(
                occurredAt,
                "occurredAt is required"
        );

        if (auditVersion <= 0) {
            throw new IllegalArgumentException(
                    "auditVersion must be positive"
            );
        }

        this.auditVersion = auditVersion;
    }

    public static ObservedCustomerAuditJpaEntity create(
            UUID auditId,
            ObservedCustomerAuditAction action,
            ObservedCustomerAuditOutcome outcome,
            UUID observedCustomerId,
            UUID paymentId,
            UUID sourceEventId,
            String actorId,
            String correlationId,
            String reasonCode,
            Instant occurredAt,
            int auditVersion
    ) {
        return new ObservedCustomerAuditJpaEntity(
                auditId,
                action,
                outcome,
                observedCustomerId,
                paymentId,
                sourceEventId,
                actorId,
                correlationId,
                reasonCode,
                occurredAt,
                auditVersion
        );
    }

    private static String requireText(
            String value,
            int maxLength,
            String label
    ) {
        Objects.requireNonNull(value, label + " is required");
        String normalized = value.strip();

        if (normalized.isEmpty() || normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    label + " must be non-blank and at most "
                            + maxLength + " characters"
            );
        }

        return normalized;
    }

    private static String normalizeReasonCode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.strip();

        if (normalized.length() > 100
                || !normalized.matches("^[A-Z0-9][A-Z0-9_.-]*$")) {
            throw new IllegalArgumentException(
                    "reasonCode must be a technical code "
                            + "of at most 100 characters"
            );
        }

        return normalized;
    }

    public UUID getAuditId() {
        return auditId;
    }

    public ObservedCustomerAuditAction getAction() {
        return action;
    }

    public ObservedCustomerAuditOutcome getOutcome() {
        return outcome;
    }

    public UUID getObservedCustomerId() {
        return observedCustomerId;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public UUID getSourceEventId() {
        return sourceEventId;
    }

    public String getActorId() {
        return actorId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public int getAuditVersion() {
        return auditVersion;
    }
}
