package com.sixpay.partner.infrastructure.idempotency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "partner_idempotency",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_partner_idempotency_operation_key",
                columnNames = {"operation", "idempotency_key"}
        )
)
public class PartnerIdempotencyJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "operation", nullable = false, updatable = false, length = 160)
    private String operation;

    @Column(name = "idempotency_key", nullable = false, updatable = false, length = 150)
    private String idempotencyKey;

    @Column(name = "partner_id", nullable = false, updatable = false)
    private UUID partnerId;

    @Column(name = "completed_at", nullable = false, updatable = false)
    private Instant completedAt;

    protected PartnerIdempotencyJpaEntity() {
    }

    public PartnerIdempotencyJpaEntity(
            String operation,
            String idempotencyKey,
            UUID partnerId,
            Instant completedAt
    ) {
        id = UUID.randomUUID();
        this.operation = operation;
        this.idempotencyKey = idempotencyKey;
        this.partnerId = partnerId;
        this.completedAt = completedAt;
    }

    public UUID partnerId() {
        return partnerId;
    }
}
