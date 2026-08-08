package com.sixpay.reporting.application.query;

import com.sixpay.reporting.domain.model.AuditResult;
import com.sixpay.reporting.domain.model.AuditSourceSystem;
import com.sixpay.reporting.domain.model.AuditTargetType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PaymentAuditRecordView(
        UUID auditId,
        Instant occurredAt,
        AuditActorView actor,
        String action,
        AuditTargetType targetType,
        String targetId,
        UUID paymentId,
        String paymentReference,
        UUID observedCustomerId,
        AuditResult result,
        String reasonCode,
        UUID correlationId,
        String traceId,
        AuditSourceSystem sourceSystem,
        String beforeState,
        String afterState,
        Map<String, Object> metadata,
        IntegrityEvidenceView integrityEvidence
) {
    public PaymentAuditRecordView {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
