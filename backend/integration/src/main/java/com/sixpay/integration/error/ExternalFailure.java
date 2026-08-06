package com.sixpay.integration.error;

import java.util.Objects;
import java.util.Optional;

public record ExternalFailure(
        String integrationId,
        String provider,
        String operation,
        ExternalErrorCategory category,
        String providerCode,
        Integer httpStatus,
        boolean retryable,
        boolean outcomeUnknown,
        String correlationId,
        String safeMessage
) {
    public ExternalFailure {
        integrationId = required(integrationId, "integrationId");
        provider = required(provider, "provider");
        operation = required(operation, "operation");
        category = Objects.requireNonNull(category, "category is required");
        providerCode = optional(providerCode);
        correlationId = required(correlationId, "correlationId");
        safeMessage = required(safeMessage, "safeMessage");
        if (outcomeUnknown && category != ExternalErrorCategory.OUTCOME_UNKNOWN) {
            throw new IllegalArgumentException("outcomeUnknown requires OUTCOME_UNKNOWN");
        }
    }
    public Optional<String> optionalProviderCode() { return Optional.ofNullable(providerCode); }
    public Optional<Integer> optionalHttpStatus() { return Optional.ofNullable(httpStatus); }
    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        return value.strip();
    }
    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
