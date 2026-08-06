package com.sixpay.integration.http;

import com.sixpay.common.context.CorrelationId;
import java.util.Objects;
import java.util.Optional;

public record IntegrationRequestContext(
        CorrelationId correlationId,
        String requestId,
        String traceParent,
        String traceState
) {
    public IntegrationRequestContext {
        correlationId = Objects.requireNonNull(correlationId, "correlationId is required");
        requestId = required(requestId, "requestId");
        traceParent = optional(traceParent);
        traceState = optional(traceState);
    }
    public Optional<String> optionalTraceParent() { return Optional.ofNullable(traceParent); }
    public Optional<String> optionalTraceState() { return Optional.ofNullable(traceState); }
    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        return value.strip();
    }
    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
