package com.sixpay.partner.events;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PartnerCreatedIntegrationEvent(
        int schemaVersion,
        UUID eventId,
        UUID partnerId,
        String legalName,
        String technicalContactEmail,
        String actorId,
        String correlationId,
        Instant occurredAt
) implements PartnerIntegrationEvent {

    public PartnerCreatedIntegrationEvent {
        if (schemaVersion != 1) {
            throw new IllegalArgumentException("unsupported schema version");
        }
        Objects.requireNonNull(eventId, "eventId is required");
        Objects.requireNonNull(partnerId, "partnerId is required");
        Objects.requireNonNull(legalName, "legalName is required");
        Objects.requireNonNull(technicalContactEmail, "technicalContactEmail is required");
        Objects.requireNonNull(actorId, "actorId is required");
        Objects.requireNonNull(correlationId, "correlationId is required");
        Objects.requireNonNull(occurredAt, "occurredAt is required");
    }
}
