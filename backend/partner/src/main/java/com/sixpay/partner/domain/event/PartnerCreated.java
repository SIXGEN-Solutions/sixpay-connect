package com.sixpay.partner.domain.event;

import com.sixpay.partner.domain.model.PartnerId;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PartnerCreated(
        UUID eventId,
        PartnerId partnerId,
        Instant occurredAt
) implements PartnerDomainEvent {

    public PartnerCreated(PartnerId partnerId, Instant occurredAt) {
        this(UUID.randomUUID(), partnerId, occurredAt);
    }

    public PartnerCreated {
        Objects.requireNonNull(eventId, "eventId is required");
        Objects.requireNonNull(partnerId, "partnerId is required");
        Objects.requireNonNull(occurredAt, "occurredAt is required");
    }
}
