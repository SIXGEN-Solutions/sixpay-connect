package com.sixpay.reporting.application.query;

import com.sixpay.reporting.domain.model.AuditEvidenceCategory;

import java.time.Instant;
import java.util.UUID;

public record TimelineCriteria(
        UUID paymentId,
        AuditEvidenceCategory category,
        Instant occurredFrom,
        Instant occurredTo,
        int size,
        Instant snapshotAt,
        AuditPosition position
) {
}
