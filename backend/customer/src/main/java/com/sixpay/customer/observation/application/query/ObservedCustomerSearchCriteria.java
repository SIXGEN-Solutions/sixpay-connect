package com.sixpay.customer.observation.application.query;

import com.sixpay.customer.observation.domain.model.ObservedPaymentStatus;

import java.time.Instant;
import java.util.Objects;

/**
 * Canonical search criteria after cursor authentication and decoding.
 */
public record ObservedCustomerSearchCriteria(
        String normalizedNiu,
        String legalName,
        String financialInstitutionCode,
        ObservedPaymentStatus lastPaymentStatus,
        String lastFailureReasonCode,
        Instant firstObservedFrom,
        Instant firstObservedTo,
        Instant lastObservedFrom,
        Instant lastObservedTo,
        Instant paymentFrom,
        Instant paymentTo,
        ObservedCustomerSort sort,
        int size,
        Instant snapshotAt,
        ObservedCustomerSearchPosition position
) {

    public ObservedCustomerSearchCriteria {
        sort = Objects.requireNonNull(
                sort,
                "sort is required"
        );
        snapshotAt = Objects.requireNonNull(
                snapshotAt,
                "snapshotAt is required"
        );

        if (size < 1 || size > SearchObservedCustomersQuery.MAX_SIZE) {
            throw new IllegalArgumentException(
                    "size must be between 1 and "
                            + SearchObservedCustomersQuery.MAX_SIZE
            );
        }
    }
}
