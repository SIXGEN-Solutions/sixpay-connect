package com.sixpay.partner.domain.event;

import com.sixpay.partner.domain.model.PartnerId;
import com.sixpay.partner.domain.model.ValidationThreshold;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PartnerThresholdConfigured(
        UUID eventId,
        PartnerId partnerId,
        ValidationThreshold threshold,
        Instant occurredAt
) implements PartnerDomainEvent {

    public PartnerThresholdConfigured(
            PartnerId partnerId,
            ValidationThreshold threshold,
            Instant occurredAt
    ) {
        this(UUID.randomUUID(), partnerId, threshold, occurredAt);
    }

    public PartnerThresholdConfigured {
        Objects.requireNonNull(eventId, "eventId is required");
        Objects.requireNonNull(partnerId, "partnerId is required");
        Objects.requireNonNull(threshold, "threshold is required");
        Objects.requireNonNull(occurredAt, "occurredAt is required");
    }
}
