package com.sixpay.payment.api;

public final class PaymentQueryRateLimitExceededException extends RuntimeException {
    private final int retryAfterSeconds;

    public PaymentQueryRateLimitExceededException(int retryAfterSeconds) {
        super("Payment query rate limit exceeded");
        if (retryAfterSeconds < 1) {
            throw new IllegalArgumentException("retryAfterSeconds must be positive");
        }
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public int retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
