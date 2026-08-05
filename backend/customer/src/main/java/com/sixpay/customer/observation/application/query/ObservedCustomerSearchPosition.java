package com.sixpay.customer.observation.application.query;

import com.sixpay.customer.observation.domain.model.ObservedCustomerId;

import java.time.Instant;
import java.util.Objects;

/**
 * Decoded keyset position for an Observed Customer search.
 */
public record ObservedCustomerSearchPosition(
        Instant lastSortValue,
        ObservedCustomerId lastObservedCustomerId
) {

    public ObservedCustomerSearchPosition {
        lastSortValue = Objects.requireNonNull(
                lastSortValue,
                "lastSortValue is required"
        );
        lastObservedCustomerId = Objects.requireNonNull(
                lastObservedCustomerId,
                "lastObservedCustomerId is required"
        );
    }
}
