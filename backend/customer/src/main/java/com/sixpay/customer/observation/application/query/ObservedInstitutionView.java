package com.sixpay.customer.observation.application.query;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Institution-level observation included input the detail view.
 */
public record ObservedInstitutionView(
        String financialInstitutionCode,
        Instant firstObservedAt,
        Instant lastObservedAt,
        List<ObservedAccountView> accounts
) {

    public ObservedInstitutionView {
        financialInstitutionCode = requiredText(
                financialInstitutionCode,
                32,
                "financialInstitutionCode"
        );

        firstObservedAt = Objects.requireNonNull(
                firstObservedAt,
                "firstObservedAt is required"
        );

        lastObservedAt = Objects.requireNonNull(
                lastObservedAt,
                "lastObservedAt is required"
        );

        if (lastObservedAt.isBefore(firstObservedAt)) {
            throw new IllegalArgumentException(
                    "lastObservedAt must not precede firstObservedAt"
            );
        }

        accounts = List.copyOf(
                Objects.requireNonNull(
                        accounts,
                        "accounts is required"
                )
        );
    }

    private static String requiredText(
            String value,
            int maxLength,
            String field
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " must not be blank"
            );
        }

        String normalized = value.strip();

        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    field + " must not exceed "
                            + maxLength
                            + " characters"
            );
        }

        return normalized;
    }
}
