package com.sixpay.customer.observation.infrastructure.resilience;

import java.util.Objects;

/**
 * Raised after the bounded projection retry policy has been exhausted.
 */
public final class ObservedCustomerProjectionRetryExhaustedException
        extends RuntimeException {

    private final int attempts;
    private final ObservedCustomerProjectionFailureType failureType;

    public ObservedCustomerProjectionRetryExhaustedException(
            int attempts,
            ObservedCustomerProjectionFailureType failureType,
            Throwable cause
    ) {
        super(
                "Observed Customer projection retry exhausted "
                        + "after "
                        + attempts
                        + " attempts; failureType="
                        + Objects.requireNonNull(
                                failureType,
                                "failureType is required"
                        ),
                Objects.requireNonNull(
                        cause,
                        "cause is required"
                )
        );

        if (attempts < 1) {
            throw new IllegalArgumentException(
                    "attempts must be positive"
            );
        }

        this.attempts = attempts;
        this.failureType = failureType;
    }

    public int attempts() {
        return attempts;
    }

    public ObservedCustomerProjectionFailureType failureType() {
        return failureType;
    }
}
