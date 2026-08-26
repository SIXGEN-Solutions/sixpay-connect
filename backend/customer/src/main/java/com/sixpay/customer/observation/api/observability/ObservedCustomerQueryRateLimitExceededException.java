package com.sixpay.customer.observation.api.observability;

public final class ObservedCustomerQueryRateLimitExceededException
        extends RuntimeException {

    public ObservedCustomerQueryRateLimitExceededException() {
        super("Observed Customer query rate limit exceeded");
    }
}
