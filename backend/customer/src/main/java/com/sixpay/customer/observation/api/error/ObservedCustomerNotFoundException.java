package com.sixpay.customer.observation.api.error;

import java.util.UUID;

public final class ObservedCustomerNotFoundException
        extends RuntimeException {

    private final UUID observedCustomerId;

    public ObservedCustomerNotFoundException(
            UUID observedCustomerId
    ) {
        super("Observed Customer was not found");
        this.observedCustomerId = observedCustomerId;
    }

    public UUID observedCustomerId() {
        return observedCustomerId;
    }
}
