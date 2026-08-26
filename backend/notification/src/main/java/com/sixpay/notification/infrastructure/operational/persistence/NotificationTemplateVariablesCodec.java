package com.sixpay.notification.infrastructure.operational.persistence;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Objects;

public final class NotificationTemplateVariablesCodec {

    private static final TypeReference<Map<String, String>> TYPE =
            new TypeReference<>() {
            };

    private final ObjectMapper objectMapper;

    public NotificationTemplateVariablesCodec(
            ObjectMapper objectMapper
    ) {
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper"
        );
    }

    public String encode(
            Map<String, String> variables
    ) {
        try {
            return objectMapper.writeValueAsString(
                    variables
            );
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(
                    "Cannot encode notification template variables",
                    exception
            );
        }
    }

    public Map<String, String> decode(
            String value
    ) {
        try {
            return Map.copyOf(
                    objectMapper.readValue(
                            value,
                            TYPE
                    )
            );
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(
                    "Cannot decode notification template variables",
                    exception
            );
        }
    }
}
