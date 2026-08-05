package com.sixpay.customer.observation.application.query;

import com.sixpay.customer.observation.domain.model.ObservedCustomerId;

import java.util.Objects;

/**
 * Reads one Observed Customer projection by its technical identifier.
 */
public record GetObservedCustomerQuery(
        ObservedCustomerId observedCustomerId
) {

    public GetObservedCustomerQuery {
        observedCustomerId = Objects.requireNonNull(
                observedCustomerId,
                "observedCustomerId is required"
        );
    }
}
