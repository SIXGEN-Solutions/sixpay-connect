package com.sixpay.integration.messaging.json;

import tools.jackson.databind.ObjectMapper;
import java.util.Objects;

public final class IntegrationJsonSerializer {
    private final ObjectMapper objectMapper;
    public IntegrationJsonSerializer(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }
    public String serialize(Object value) {
        Objects.requireNonNull(value, "value is required");
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IntegrationSerializationException("Unable to serialize integration payload", e);
        }
    }
    public <T> T deserialize(String value, Class<T> type) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("value is required");
        Objects.requireNonNull(type, "type is required");
        try {
            return objectMapper.readValue(value, type);
        } catch (Exception e) {
            throw new IntegrationSerializationException("Unable to deserialize integration payload", e);
        }
    }
}
