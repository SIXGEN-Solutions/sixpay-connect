package com.sixpay.administration.domain.model;

import java.util.Set;

public record SettingDefinition(
        String key,
        SettingDomain domain,
        SettingValueType type,
        SettingClassification classification,
        String defaultValue,
        String minimumValue,
        String maximumValue,
        Set<String> allowedValues,
        String description,
        boolean dynamic,
        boolean sensitive,
        boolean requiresRestart,
        String validator
) {
    public SettingDefinition {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("key is required");
        if (domain == null) throw new IllegalArgumentException("domain is required");
        if (type == null) throw new IllegalArgumentException("type is required");
        if (classification == null) throw new IllegalArgumentException("classification is required");
        if (defaultValue == null || defaultValue.isBlank()) throw new IllegalArgumentException("defaultValue is required");
        if (description == null || description.isBlank()) throw new IllegalArgumentException("description is required");
        if (dynamic != classification.dynamicallyMutable()) {
            throw new IllegalArgumentException("dynamic must match classification mutability for " + key);
        }
        if (sensitive && classification != SettingClassification.SECRET) {
            throw new IllegalArgumentException("sensitive settings must be classified SECRET");
        }
        if (classification == SettingClassification.SECRET && dynamic) {
            throw new IllegalArgumentException("SECRET settings cannot be dynamic");
        }

        key = key.strip();
        minimumValue = normalizeNullable(minimumValue);
        maximumValue = normalizeNullable(maximumValue);
        description = description.strip();
        validator = normalizeNullable(validator);
        allowedValues = allowedValues == null ? Set.of() : Set.copyOf(allowedValues);
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
