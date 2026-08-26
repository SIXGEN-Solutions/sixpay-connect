package com.sixpay.partner.application.port.output;

import com.sixpay.partner.domain.model.PartnerId;

import java.time.Instant;
import java.util.Objects;

public record PartnerAuditRecord(
        PartnerId partnerId,
        String action,
        String result,
        String actorId,
        String correlationId,
        String details,
        Instant occurredAt
) {

    public PartnerAuditRecord {
        Objects.requireNonNull(partnerId, "partnerId is required");
        action = requireText(action, "action");
        result = requireText(result, "result");
        actorId = requireText(actorId, "actorId");
        correlationId = requireText(correlationId, "correlationId");
        details = requireText(details, "details");
        Objects.requireNonNull(occurredAt, "occurredAt is required");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.strip();
    }
}
