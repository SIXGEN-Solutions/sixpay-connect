package com.sixpay.partner.application.port.output;

import com.sixpay.partner.domain.model.PartnerId;
import com.sixpay.partner.domain.model.ValidationThreshold;

import java.time.Instant;
import java.util.Objects;

public record PartnerThresholdHistoryRecord(
        PartnerId partnerId,
        ValidationThreshold previousThreshold,
        ValidationThreshold currentThreshold,
        String actorId,
        String correlationId,
        Instant changedAt
) {

    public PartnerThresholdHistoryRecord {
        Objects.requireNonNull(partnerId, "partnerId is required");
        Objects.requireNonNull(
                currentThreshold,
                "currentThreshold is required"
        );
        actorId = requireText(actorId, "actorId");
        correlationId = requireText(correlationId, "correlationId");
        Objects.requireNonNull(changedAt, "changedAt is required");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.strip();
    }
}
