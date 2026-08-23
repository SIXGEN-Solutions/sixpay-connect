package com.sixpay.customer.management.infrastructure.audit;

import com.sixpay.customer.management.application.audit.CustomerAuditRecord;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customer_management_audit")
public class CustomerAuditJpaEntity {

    @Id
    @Column(name = "audit_id", nullable = false, updatable = false)
    private UUID auditId;

    @Column(name = "aggregate_type", nullable = false, length = 64)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "result", nullable = false, length = 32)
    private String result;

    @Column(name = "actor_id", nullable = false, length = 200)
    private String actorId;

    @Column(name = "correlation_id", nullable = false, length = 150)
    private String correlationId;

    @Column(name = "details", nullable = false, length = 2000)
    private String details;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected CustomerAuditJpaEntity() {
    }

    CustomerAuditJpaEntity(CustomerAuditRecord record) {
        auditId = record.auditId();
        aggregateType = record.aggregateType();
        aggregateId = record.aggregateId();
        action = record.action();
        result = record.result();
        actorId = record.actorId();
        correlationId = record.correlationId();
        details = record.details();
        occurredAt = record.occurredAt();
    }

    CustomerAuditRecord toRecord() {
        return new CustomerAuditRecord(
                auditId,
                aggregateType,
                aggregateId,
                action,
                result,
                actorId,
                correlationId,
                details,
                occurredAt
        );
    }
}
