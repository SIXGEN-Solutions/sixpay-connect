package com.sixpay.administration.api.dto;

import com.sixpay.administration.domain.model.IncidentSeverity;
import com.sixpay.administration.domain.model.IncidentStatus;
import com.sixpay.administration.domain.model.OperationalIncident;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record IncidentDetailResponse(
        String incidentId,
        IncidentSeverity severity,
        String component,
        String summary,
        IncidentStatus status,
        Instant openedAt,
        Instant updatedAt,
        String description,
        String impact,
        UUID accountingBatchId,
        UUID paymentId,
        String paymentReference,
        UUID correlationId,
        List<IncidentTimelineEntryResponse> timeline
) {
    public static IncidentDetailResponse from(
            OperationalIncident incident
    ) {
        return new IncidentDetailResponse(
                incident.incidentId().value(),
                incident.severity(),
                incident.component(),
                incident.summary(),
                incident.status(),
                incident.openedAt(),
                incident.updatedAt(),
                incident.description(),
                incident.impact(),
                incident.accountingBatchId(),
                incident.paymentId(),
                incident.paymentReference(),
                incident.correlationId(),
                incident.timeline()
                        .stream()
                        .map(
                                IncidentTimelineEntryResponse::from
                        )
                        .toList()
        );
    }
}
