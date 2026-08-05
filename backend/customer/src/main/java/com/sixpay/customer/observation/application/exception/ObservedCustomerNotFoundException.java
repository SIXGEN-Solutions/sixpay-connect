package com.sixpay.customer.observation.application.exception;

import com.sixpay.customer.observation.domain.model
        .ObservedCustomerId;

import java.util.Objects;

/**
 * Raised when an Observed Customer projection is not visible or does not
 * exist.
 */
public final class ObservedCustomerNotFoundException
        extends RuntimeException {

    private final ObservedCustomerId observedCustomerId;

    public ObservedCustomerNotFoundException(
            ObservedCustomerId observedCustomerId
    ) {
        super(
                "Observed Customer projection was not found"
        );
        this.observedCustomerId = Objects.requireNonNull(
                observedCustomerId,
                "observedCustomerId is required"
        );
    }

    public ObservedCustomerId observedCustomerId() {
        return observedCustomerId;
    }
}
