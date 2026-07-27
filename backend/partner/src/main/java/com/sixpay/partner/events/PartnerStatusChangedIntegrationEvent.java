package com.sixpay.partner.events;

import com.sixpay.partner.domain.model.PartnerStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PartnerStatusChangedIntegrationEvent(
        int schemaVersion,
        UUID eventId,
        UUID partnerId,
        PartnerStatus previousStatus,
        PartnerStatus currentStatus,
        String reason,
        String actorId,
        String correlationId,
        Instant occurredAt
) implements PartnerIntegrationEvent {

    public PartnerStatusChangedIntegrationEvent {
        if (schemaVersion != 1) {
            throw new IllegalArgumentException("unsupported schema version");
        }
        Objects.requireNonNull(eventId, "eventId is required");
        Objects.requireNonNull(partnerId, "partnerId is required");
        Objects.requireNonNull(previousStatus, "previousStatus is required");
        Objects.requireNonNull(currentStatus, "currentStatus is required");
        Objects.requireNonNull(actorId, "actorId is required");
        Objects.requireNonNull(correlationId, "correlationId is required");
        Objects.requireNonNull(occurredAt, "occurredAt is required");
    }
}
