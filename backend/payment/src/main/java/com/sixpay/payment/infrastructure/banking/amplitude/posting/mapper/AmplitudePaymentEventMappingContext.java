package com.sixpay.payment.infrastructure.banking.amplitude.posting.mapper;

import java.time.Instant;
import java.time.LocalDate;

public record AmplitudePaymentEventMappingContext(
        String operationCode,
        long eventNumber,
        LocalDate accountingDate,
        boolean nightMode,
        String nature,
        String technicalUser,
        Instant requestedAt
) {
    public AmplitudePaymentEventMappingContext {
        operationCode = requireText(
                operationCode,
                "Operation code"
        );
        if (operationCode.length() > 3) {
            throw new IllegalArgumentException(
                    "Operation code must contain at most 3 characters"
            );
        }
        if (eventNumber <= 0) {
            throw new IllegalArgumentException(
                    "Event number must be positive"
            );
        }
        if (accountingDate == null) {
            throw new IllegalArgumentException(
                    "Accounting date is required"
            );
        }
        nature = requireText(
                nature,
                "Amplitude nature"
        );
        if (nature.length() > 6) {
            throw new IllegalArgumentException(
                    "Amplitude nature must contain at most 6 characters"
            );
        }
        technicalUser = requireText(
                technicalUser,
                "Amplitude technical user"
        );
        if (requestedAt == null) {
            throw new IllegalArgumentException(
                    "Requested at is required"
            );
        }
    }

    private static String requireText(
            String value,
            String label
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    label + " must not be blank"
            );
        }
        return value;
    }
}
