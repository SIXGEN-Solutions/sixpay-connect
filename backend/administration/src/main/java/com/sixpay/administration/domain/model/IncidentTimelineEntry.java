package com.sixpay.administration.domain.model;

import java.time.Instant;
import java.util.Objects;

public record IncidentTimelineEntry(
        String eventId,
        Instant occurredAt,
        String message,
        String actor,
        int sequenceNo
) {
    public IncidentTimelineEntry {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(actor, "actor");

        if (eventId.isBlank()) {
            throw new IllegalArgumentException(
                    "Timeline event id must not be blank"
            );
        }

        if (eventId.length() > 64) {
            throw new IllegalArgumentException(
                    "Timeline event id must not exceed 64 characters"
            );
        }

        if (message.isBlank()) {
            throw new IllegalArgumentException(
                    "Timeline message must not be blank"
            );
        }

        if (actor.isBlank()) {
            throw new IllegalArgumentException(
                    "Timeline actor must not be blank"
            );
        }

        if (sequenceNo < 0) {
            throw new IllegalArgumentException(
                    "Timeline sequence must be non-negative"
            );
        }
    }
}
