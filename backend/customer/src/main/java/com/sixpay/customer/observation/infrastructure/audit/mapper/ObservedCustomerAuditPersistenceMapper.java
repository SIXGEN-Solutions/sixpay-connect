package com.sixpay.customer.observation.infrastructure.audit.mapper;

import com.sixpay.customer.observation.application.audit
        .ObservedCustomerAuditRecord;
import com.sixpay.customer.observation.infrastructure.audit.entity
        .ObservedCustomerAuditJpaEntity;

import java.util.Objects;

public final class ObservedCustomerAuditPersistenceMapper {

    public static final int CURRENT_AUDIT_VERSION = 1;

    public ObservedCustomerAuditJpaEntity toEntity(
            ObservedCustomerAuditRecord record
    ) {
        Objects.requireNonNull(record, "record is required");

        return ObservedCustomerAuditJpaEntity.create(
                record.auditId(),
                record.action(),
                record.outcome(),
                record.observedCustomerId() == null
                        ? null
                        : record.observedCustomerId().value(),
                record.paymentId(),
                record.sourceEventId(),
                record.actorId(),
                record.correlationId(),
                record.reasonCode(),
                record.occurredAt(),
                CURRENT_AUDIT_VERSION
        );
    }
}
