package com.sixpay.partner.domain.event;

import com.sixpay.partner.domain.model.PartnerId;
import com.sixpay.partner.domain.model.PartnerStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PartnerStatusChanged(
        UUID eventId,
        PartnerId partnerId,
        PartnerStatus previousStatus,
        PartnerStatus currentStatus,
        String reason,
        Instant occurredAt
) implements PartnerDomainEvent {

    public PartnerStatusChanged(
            PartnerId partnerId,
            PartnerStatus previousStatus,
            PartnerStatus currentStatus,
            String reason,
            Instant occurredAt
    ) {
        this(
                UUID.randomUUID(),
                partnerId,
                previousStatus,
                currentStatus,
                reason,
                occurredAt
        );
    }

    public PartnerStatusChanged {
        Objects.requireNonNull(eventId, "eventId is required");
        Objects.requireNonNull(partnerId, "partnerId is required");
        Objects.requireNonNull(previousStatus, "previousStatus is required");
        Objects.requireNonNull(currentStatus, "currentStatus is required");
        Objects.requireNonNull(occurredAt, "occurredAt is required");
    }
}
