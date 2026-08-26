package com.sixpay.customer.management.api.response;

import com.sixpay.customer.management.application.audit.CustomerAuditRecord;

import java.time.Instant;
import java.util.UUID;

public record CustomerAuditRecordResponse(
        UUID auditId,
        String aggregateType,
        UUID aggregateId,
        String action,
        String result,
        String actorId,
        String correlationId,
        String details,
        Instant occurredAt
) {
    public static CustomerAuditRecordResponse from(
            CustomerAuditRecord record
    ) {
        return new CustomerAuditRecordResponse(
                record.auditId(),
                record.aggregateType(),
                record.aggregateId(),
                record.action(),
                record.result(),
                record.actorId(),
                record.correlationId(),
                record.details(),
                record.occurredAt()
        );
    }
}
