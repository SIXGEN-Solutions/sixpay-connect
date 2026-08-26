package com.sixpay.reporting.application.query;

import com.sixpay.reporting.domain.model.AuditActorType;
import com.sixpay.reporting.domain.model.AuditResult;
import com.sixpay.reporting.domain.model.AuditSort;
import com.sixpay.reporting.domain.model.AuditSourceSystem;

import java.time.Instant;
import java.util.UUID;

public record AuditSearchCriteria(
        UUID paymentId,
        String paymentReference,
        UUID observedCustomerId,
        String actorId,
        AuditActorType actorType,
        String action,
        AuditResult result,
        String reasonCode,
        UUID correlationId,
        AuditSourceSystem sourceSystem,
        Instant occurredFrom,
        Instant occurredTo,
        AuditSort sort,
        int size,
        Instant snapshotAt,
        AuditPosition position
) {
}
