package com.sixpay.customer.verification.infrastructure.banking.error;

import java.time.Duration;

public final class AmplitudeRateLimitException
        extends RuntimeException {

    private final Duration retryAfter;

    public AmplitudeRateLimitException(
            Duration retryAfter,
            Throwable cause
    ) {
        super("Core Banking rate limit exceeded", cause);
        this.retryAfter = retryAfter;
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}
