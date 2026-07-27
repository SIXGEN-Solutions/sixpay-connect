package com.sixpay.partner.infrastructure.audit;

import com.sixpay.partner.application.port.out.PartnerAuditRecord;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "partner_audit")
public class PartnerAuditJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "partner_id", nullable = false, updatable = false)
    private UUID partnerId;

    @Column(name = "action", nullable = false, updatable = false, length = 64)
    private String action;

    @Column(name = "result", nullable = false, updatable = false, length = 32)
    private String result;

    @Column(name = "actor_id", nullable = false, updatable = false, length = 150)
    private String actorId;

    @Column(name = "correlation_id", nullable = false, updatable = false, length = 150)
    private String correlationId;

    @Column(name = "details", nullable = false, updatable = false, length = 1000)
    private String details;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected PartnerAuditJpaEntity() {
    }

    public PartnerAuditJpaEntity(PartnerAuditRecord record) {
        id = UUID.randomUUID();
        partnerId = record.partnerId().value();
        action = record.action();
        result = record.result();
        actorId = record.actorId();
        correlationId = record.correlationId();
        details = record.details();
        occurredAt = record.occurredAt();
    }

    public PartnerAuditRecord toRecord() {
        return new PartnerAuditRecord(
                new com.sixpay.partner.domain.model.PartnerId(partnerId),
                action,
                result,
                actorId,
                correlationId,
                details,
                occurredAt
        );
    }
}
