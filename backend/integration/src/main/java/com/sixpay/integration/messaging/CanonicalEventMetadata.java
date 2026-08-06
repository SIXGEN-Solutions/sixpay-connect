package com.sixpay.integration.messaging;

import java.util.Map;
import java.util.Optional;

public record CanonicalEventMetadata(
        String producer,
        String causationId,
        String contentType,
        Map<String, String> attributes
) {
    public CanonicalEventMetadata {
        producer = required(producer, "producer");
        causationId = optional(causationId);
        contentType = required(contentType, "contentType");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
    public Optional<String> optionalCausationId() { return Optional.ofNullable(causationId); }
    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        return value.strip();
    }
    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
