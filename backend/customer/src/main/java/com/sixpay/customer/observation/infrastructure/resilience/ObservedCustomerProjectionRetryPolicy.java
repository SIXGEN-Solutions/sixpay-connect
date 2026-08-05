package com.sixpay.customer.observation.infrastructure.resilience;

import java.time.Duration;
import java.util.Objects;
import java.util.function.DoubleSupplier;

/**
 * Computes bounded exponential backoff and delegates waiting to an injected
 * infrastructure component.
 */
public final class ObservedCustomerProjectionRetryPolicy {

    private final int maxAttempts;
    private final Duration initialBackoff;
    private final Duration maxBackoff;
    private final double multiplier;
    private final double jitter;
    private final ObservedCustomerProjectionBackoff backoff;
    private final DoubleSupplier random;

    public ObservedCustomerProjectionRetryPolicy(
            int maxAttempts,
            Duration initialBackoff,
            Duration maxBackoff,
            double multiplier,
            double jitter,
            ObservedCustomerProjectionBackoff backoff,
            DoubleSupplier random
    ) {
        if (maxAttempts < 1 || maxAttempts > 10) {
            throw new IllegalArgumentException(
                    "maxAttempts must be between 1 and 10"
            );
        }

        this.initialBackoff = requireDuration(
                initialBackoff,
                "initialBackoff"
        );
        this.maxBackoff = requireDuration(
                maxBackoff,
                "maxBackoff"
        );

        if (this.maxBackoff.compareTo(
                this.initialBackoff
        ) < 0) {
            throw new IllegalArgumentException(
                    "maxBackoff must not be less than "
                            + "initialBackoff"
            );
        }

        if (!Double.isFinite(multiplier)
                || multiplier < 1.0
                || multiplier > 10.0) {
            throw new IllegalArgumentException(
                    "multiplier must be between 1 and 10"
            );
        }

        if (!Double.isFinite(jitter)
                || jitter < 0.0
                || jitter > 1.0) {
            throw new IllegalArgumentException(
                    "jitter must be between 0 and 1"
            );
        }

        this.maxAttempts = maxAttempts;
        this.multiplier = multiplier;
        this.jitter = jitter;
        this.backoff = Objects.requireNonNull(
                backoff,
                "backoff is required"
        );
        this.random = Objects.requireNonNull(
                random,
                "random is required"
        );
    }

    public int maxAttempts() {
        return maxAttempts;
    }

    public boolean shouldRetry(
            int failedAttempt,
            ObservedCustomerProjectionFailureType failureType
    ) {
        Objects.requireNonNull(
                failureType,
                "failureType is required"
        );

        return failureType.retryable()
                && failedAttempt < maxAttempts;
    }

    public void beforeRetry(int failedAttempt) {
        backoff.pause(delayAfter(failedAttempt));
    }

    public Duration delayAfter(int failedAttempt) {
        if (failedAttempt < 1) {
            throw new IllegalArgumentException(
                    "failedAttempt must be positive"
            );
        }

        double exponential =
                initialBackoff.toNanos()
                        * Math.pow(
                        multiplier,
                        failedAttempt - 1.0
                );

        long boundedNanos = Math.min(
                maxBackoff.toNanos(),
                safeNanos(exponential)
        );

        double randomValue = random.getAsDouble();

        if (!Double.isFinite(randomValue)
                || randomValue < 0.0
                || randomValue >= 1.0) {
            throw new IllegalStateException(
                    "random supplier must return a value "
                            + "between 0 inclusive and 1 exclusive"
            );
        }

        double jitterFactor =
                1.0 - jitter
                        + (2.0 * jitter * randomValue);

        long jitteredNanos = Math.max(
                0L,
                Math.min(
                        maxBackoff.toNanos(),
                        safeNanos(
                                boundedNanos * jitterFactor
                        )
                )
        );

        return Duration.ofNanos(jitteredNanos);
    }

    private static Duration requireDuration(
            Duration value,
            String name
    ) {
        Objects.requireNonNull(value, name + " is required");

        if (value.isNegative() || value.isZero()) {
            throw new IllegalArgumentException(
                    name + " must be positive"
            );
        }

        return value;
    }

    private static long safeNanos(double value) {
        if (value >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }

        return Math.max(0L, Math.round(value));
    }
}
