package com.sixpay.payment.infrastructure.banking.amplitude.posting.mapper;

import java.time.Instant;
import java.util.Objects;

public record AmplitudePaymentEventMappingContext(
        String operationCode, String nature, String technicalUser, Instant requestedAt
) {
    public AmplitudePaymentEventMappingContext {
        if (operationCode == null || operationCode.isBlank() || operationCode.length() > 3) {
            throw new IllegalArgumentException("Operation code must be 1..3 characters");
        }
        if (nature == null || nature.isBlank()) {
            throw new IllegalArgumentException("Nature is required");
        }
        if (technicalUser == null || technicalUser.isBlank()) {
            throw new IllegalArgumentException("Technical user is required");
        }
        requestedAt = Objects.requireNonNull(requestedAt, "Requested at");
    }
}
