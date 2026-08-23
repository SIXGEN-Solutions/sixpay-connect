package com.sixpay.administration.api.dto;

import com.sixpay.administration.domain.model.IncidentSeverity;
import com.sixpay.administration.domain.model.IncidentStatus;
import com.sixpay.administration.domain.model.OperationalIncident;

import java.time.Instant;

public record IncidentSummaryResponse(
        String incidentId,
        IncidentSeverity severity,
        String component,
        String summary,
        IncidentStatus status,
        Instant openedAt,
        Instant updatedAt
) {
    public static IncidentSummaryResponse from(
            OperationalIncident incident
    ) {
        return new IncidentSummaryResponse(
                incident.incidentId().value(),
                incident.severity(),
                incident.component(),
                incident.summary(),
                incident.status(),
                incident.openedAt(),
                incident.updatedAt()
        );
    }
}
