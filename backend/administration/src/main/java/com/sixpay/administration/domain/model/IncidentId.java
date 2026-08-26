package com.sixpay.administration.domain.model;

import java.util.Objects;

public record IncidentId(String value) {

    public IncidentId {
        Objects.requireNonNull(value, "value");

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    "Incident id must not be blank"
            );
        }

        if (value.length() > 64) {
            throw new IllegalArgumentException(
                    "Incident id must not exceed 64 characters"
            );
        }
    }
}
