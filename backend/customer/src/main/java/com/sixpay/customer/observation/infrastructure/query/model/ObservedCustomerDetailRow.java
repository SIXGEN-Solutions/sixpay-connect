package com.sixpay.customer.observation.infrastructure.query.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Complete SQL projection row used by the detail mapper.
 */
public record ObservedCustomerDetailRow(
        UUID observedCustomerId,
        String niuProtected,
        String legalNameProtected,
        String phoneMasked,
        String emailMasked,
        List<ObservedInstitutionRow> institutions,
        Instant firstObservedAt,
        Instant lastObservedAt,
        long totalPayments,
        long successfulPayments,
        long failedPayments,
        String lastPaymentStatus,
        String lastFailureReasonCode,
        Instant updatedAt,
        long projectionVersion,
        String sourceEventWatermark
) {

    public ObservedCustomerDetailRow {
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
        institutions = List.copyOf(
                Objects.requireNonNull(
                        institutions,
                        "institutions is required"
                )
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
        sourceEventWatermark = Objects.requireNonNull(
                sourceEventWatermark,
                "sourceEventWatermark is required"
        );
    }
}
