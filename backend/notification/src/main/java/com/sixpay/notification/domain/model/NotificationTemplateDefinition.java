package com.sixpay.notification.domain.model;

import java.util.Objects;
import java.util.Set;

public record NotificationTemplateDefinition(
        NotificationTemplateKey key,
        String resourcePath,
        Set<String> allowedVariables
) {
    public NotificationTemplateDefinition {
        key = Objects.requireNonNull(
                key,
                "key"
        );

        if (resourcePath == null
                || resourcePath.isBlank()) {
            throw new IllegalArgumentException(
                    "resourcePath is required"
            );
        }

        resourcePath = resourcePath.strip();

        allowedVariables = Set.copyOf(
                Objects.requireNonNull(
                        allowedVariables,
                        "allowedVariables"
                )
        );
    }
}
