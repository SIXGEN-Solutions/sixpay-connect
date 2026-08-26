package com.sixpay.customer.observation.infrastructure.query.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Flat SQL projection row for Observed Customer search results.
 */
public record ObservedCustomerSummaryRow(
        UUID observedCustomerId,
        String niuProtected,
        String legalNameProtected,
        String phoneMasked,
        String emailMasked,
        Instant firstObservedAt,
        Instant lastObservedAt,
        long totalPayments,
        long successfulPayments,
        long failedPayments,
        String lastPaymentStatus,
        String lastFailureReasonCode,
        Instant updatedAt,
        long projectionVersion
) {

    public ObservedCustomerSummaryRow {
        observedCustomerId = Objects.requireNonNull(
                observedCustomerId,
                "observedCustomerId is required"
        );
        niuProtected = Objects.requireNonNull(
                niuProtected,
                "niuProtected is required"
        );
        legalNameProtected = Objects.requireNonNull(
                legalNameProtected,
                "legalNameProtected is required"
        );
        firstObservedAt = Objects.requireNonNull(
                firstObservedAt,
                "firstObservedAt is required"
        );
        lastObservedAt = Objects.requireNonNull(
                lastObservedAt,
                "lastObservedAt is required"
        );
        updatedAt = Objects.requireNonNull(
                updatedAt,
                "updatedAt is required"
        );
    }
}
