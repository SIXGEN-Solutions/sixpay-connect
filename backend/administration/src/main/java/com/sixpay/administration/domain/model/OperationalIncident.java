package com.sixpay.administration.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record OperationalIncident(
        IncidentId incidentId,
        IncidentSeverity severity,
        String component,
        String summary,
        IncidentStatus status,
        String description,
        String impact,
        UUID accountingBatchId,
        UUID paymentId,
        String paymentReference,
        UUID correlationId,
        Instant openedAt,
        Instant updatedAt,
        List<IncidentTimelineEntry> timeline
) {
    public OperationalIncident {
        Objects.requireNonNull(incidentId, "incidentId");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(component, "component");
        Objects.requireNonNull(summary, "summary");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(impact, "impact");
        Objects.requireNonNull(openedAt, "openedAt");
        Objects.requireNonNull(updatedAt, "updatedAt");

        timeline = List.copyOf(
                Objects.requireNonNull(timeline, "timeline")
        );

        if (component.isBlank()) {
            throw new IllegalArgumentException(
                    "Incident component must not be blank"
            );
        }

        if (summary.isBlank()) {
            throw new IllegalArgumentException(
                    "Incident summary must not be blank"
            );
        }

        if (description.isBlank()) {
            throw new IllegalArgumentException(
                    "Incident description must not be blank"
            );
        }

        if (impact.isBlank()) {
            throw new IllegalArgumentException(
                    "Incident impact must not be blank"
            );
        }

        if (updatedAt.isBefore(openedAt)) {
            throw new IllegalArgumentException(
                    "updatedAt must not be before openedAt"
            );
        }
    }
}
