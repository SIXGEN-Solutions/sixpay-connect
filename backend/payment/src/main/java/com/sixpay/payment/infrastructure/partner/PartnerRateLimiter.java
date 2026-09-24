package com.sixpay.payment.infrastructure.partner;

public interface PartnerRateLimiter {

    RateLimitDecision acquire(String partnerId);

    record RateLimitDecision(
            boolean allowed,
            int retryAfterSeconds
    ) {
        public RateLimitDecision {
            if (allowed && retryAfterSeconds != 0) {
                throw new IllegalArgumentException(
                        "Allowed decisions must have retryAfterSeconds = 0"
                );
            }
            if (!allowed && retryAfterSeconds < 1) {
                throw new IllegalArgumentException(
                        "Rejected decisions require a positive retryAfterSeconds"
                );
            }
        }

        public static RateLimitDecision permit() {
            return new RateLimitDecision(true, 0);
        }

        public static RateLimitDecision reject(int retryAfterSeconds) {
            return new RateLimitDecision(
                    false,
                    Math.max(1, retryAfterSeconds)
            );
        }
    }
}
