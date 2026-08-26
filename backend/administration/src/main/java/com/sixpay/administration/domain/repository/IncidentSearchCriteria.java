package com.sixpay.administration.domain.repository;

import com.sixpay.administration.domain.model.IncidentSeverity;
import com.sixpay.administration.domain.model.IncidentStatus;

public record IncidentSearchCriteria(
        IncidentSeverity severity,
        IncidentStatus status,
        String component,
        int page,
        int size
) {
    public IncidentSearchCriteria {
        if (page < 0) {
            throw new IllegalArgumentException(
                    "page must be >= 0"
            );
        }

        if (size < 1 || size > 200) {
            throw new IllegalArgumentException(
                    "size must be between 1 and 200"
            );
        }

        component = normalize(component);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}
