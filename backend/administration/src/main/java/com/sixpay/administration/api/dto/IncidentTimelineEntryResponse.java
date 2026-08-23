package com.sixpay.administration.api.dto;

import com.sixpay.administration.domain.model.IncidentTimelineEntry;

import java.time.Instant;

public record IncidentTimelineEntryResponse(
        String eventId,
        Instant occurredAt,
        String message,
        String actor
) {
    public static IncidentTimelineEntryResponse from(
            IncidentTimelineEntry entry
    ) {
        return new IncidentTimelineEntryResponse(
                entry.eventId(),
                entry.occurredAt(),
                entry.message(),
                entry.actor()
        );
    }
}
