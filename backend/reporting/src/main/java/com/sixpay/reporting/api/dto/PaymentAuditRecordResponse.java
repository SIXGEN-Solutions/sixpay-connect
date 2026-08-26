package com.sixpay.reporting.api.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PaymentAuditRecordResponse(
        UUID auditId,
        Instant occurredAt,
        AuditActorResponse actor,
        String action,
        String targetType,
        String targetId,
        UUID paymentId,
        String paymentReference,
        UUID observedCustomerId,
        String result,
        String reasonCode,
        UUID correlationId,
        String traceId,
        String sourceSystem,
        String beforeState,
        String afterState,
        Map<String, Object> metadata,
        IntegrityEvidenceResponse integrityEvidence
) {
}
