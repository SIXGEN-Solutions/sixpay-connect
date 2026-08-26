package com.sixpay.accounting.domain.model;

import java.time.Instant;
import java.util.Objects;

public record TresorPayPaymentStatusEvidence(
        String providerStatus,
        Instant checkedAt,
        String requestReference,
        String correlationId
) {
    public TresorPayPaymentStatusEvidence {
        providerStatus = required(providerStatus, "providerStatus");
        checkedAt = Objects.requireNonNull(checkedAt, "checkedAt");
        requestReference = required(requestReference, "requestReference");
        correlationId = required(correlationId, "correlationId");
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.strip();
    }
}
