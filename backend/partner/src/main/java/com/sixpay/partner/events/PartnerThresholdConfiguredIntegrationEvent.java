package com.sixpay.partner.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PartnerThresholdConfiguredIntegrationEvent(
        int schemaVersion,
        UUID eventId,
        UUID partnerId,
        String transactionType,
        String currency,
        BigDecimal amount,
        int validationLevels,
        String actorId,
        String correlationId,
        Instant occurredAt
) implements PartnerIntegrationEvent {

    public PartnerThresholdConfiguredIntegrationEvent {
        if (schemaVersion != 1) {
            throw new IllegalArgumentException("unsupported schema version");
        }
        Objects.requireNonNull(eventId, "eventId is required");
        Objects.requireNonNull(partnerId, "partnerId is required");
        Objects.requireNonNull(transactionType, "transactionType is required");
        Objects.requireNonNull(currency, "currency is required");
        Objects.requireNonNull(amount, "amount is required");
        Objects.requireNonNull(actorId, "actorId is required");
        Objects.requireNonNull(correlationId, "correlationId is required");
        Objects.requireNonNull(occurredAt, "occurredAt is required");
    }
}
