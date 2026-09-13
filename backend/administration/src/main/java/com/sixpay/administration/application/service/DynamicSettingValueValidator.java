package com.sixpay.administration.application.service;

import com.sixpay.administration.domain.model.SettingDefinition;
import java.math.BigDecimal;
import java.time.Duration;

public final class DynamicSettingValueValidator {

    public void validate(SettingDefinition definition, String rawValue) {
        if (definition == null) throw new IllegalArgumentException("definition is required");
        if (!definition.dynamic()) {
            throw new IllegalArgumentException("setting is not dynamically mutable: " + definition.key());
        }
        if (rawValue == null || rawValue.isBlank()) throw new IllegalArgumentException("value is required");

        String value = rawValue.strip();
        if (!definition.allowedValues().isEmpty() && !definition.allowedValues().contains(value)) {
            throw new IllegalArgumentException("value is not allowed for " + definition.key());
        }

        switch (definition.type()) {
            case BOOLEAN -> validateBoolean(definition, value);
            case INTEGER -> validateInteger(definition, value);
            case DECIMAL -> validateDecimal(definition, value);
            case DURATION -> validateDuration(definition, value);
            case STRING -> validateString(definition, value);
        }
    }

    private void validateBoolean(SettingDefinition definition, String value) {
        if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
            throw new IllegalArgumentException("invalid boolean value for " + definition.key());
        }
    }

    private void validateInteger(SettingDefinition definition, String value) {
        try {
            long parsed = Long.parseLong(value);
            if (definition.minimumValue() != null && parsed < Long.parseLong(definition.minimumValue())) throw outOfRange(definition);
            if (definition.maximumValue() != null && parsed > Long.parseLong(definition.maximumValue())) throw outOfRange(definition);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("invalid integer value for " + definition.key(), ex);
        }
    }

    private void validateDecimal(SettingDefinition definition, String value) {
        try {
            BigDecimal parsed = new BigDecimal(value);
            if (definition.minimumValue() != null && parsed.compareTo(new BigDecimal(definition.minimumValue())) < 0) throw outOfRange(definition);
            if (definition.maximumValue() != null && parsed.compareTo(new BigDecimal(definition.maximumValue())) > 0) throw outOfRange(definition);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("invalid decimal value for " + definition.key(), ex);
        }
    }

    private void validateDuration(SettingDefinition definition, String value) {
        try {
            Duration parsed = Duration.parse(value);
            if (definition.minimumValue() != null && parsed.compareTo(Duration.parse(definition.minimumValue())) < 0) throw outOfRange(definition);
            if (definition.maximumValue() != null && parsed.compareTo(Duration.parse(definition.maximumValue())) > 0) throw outOfRange(definition);
        } catch (RuntimeException ex) {
            if (ex instanceof IllegalArgumentException
                    && ex.getMessage() != null
                    && ex.getMessage().startsWith("value is outside")) {
                throw ex;
            }
            throw new IllegalArgumentException("invalid duration value for " + definition.key(), ex);
        }
    }

    private void validateString(SettingDefinition definition, String value) {
        if (definition.minimumValue() != null && value.length() < Integer.parseInt(definition.minimumValue())) throw outOfRange(definition);
        if (definition.maximumValue() != null && value.length() > Integer.parseInt(definition.maximumValue())) throw outOfRange(definition);
    }

    private IllegalArgumentException outOfRange(SettingDefinition definition) {
        return new IllegalArgumentException("value is outside configured bounds for " + definition.key());
    }
}
