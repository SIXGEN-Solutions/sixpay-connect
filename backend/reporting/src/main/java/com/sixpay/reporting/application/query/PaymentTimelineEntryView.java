package com.sixpay.reporting.application.query;

import com.sixpay.reporting.domain.model.AuditEvidenceCategory;
import com.sixpay.reporting.domain.model.AuditSourceSystem;
import com.sixpay.reporting.domain.model.TimelineResult;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PaymentTimelineEntryView(
        UUID timelineEntryId,
        UUID paymentId,
        AuditEvidenceCategory category,
        String eventType,
        String fromState,
        String toState,
        TimelineResult result,
        String reasonCode,
        Instant occurredAt,
        UUID correlationId,
        AuditSourceSystem sourceSystem,
        String externalReference,
        long aggregateVersion,
        Map<String, Object> metadata
) {
    public PaymentTimelineEntryView {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
