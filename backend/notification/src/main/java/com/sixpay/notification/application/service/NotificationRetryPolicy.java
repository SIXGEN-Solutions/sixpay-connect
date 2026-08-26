package com.sixpay.notification.application.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class NotificationRetryPolicy {

    private final int maxAttempts;
    private final Duration initialDelay;
    private final double multiplier;
    private final Duration maxDelay;

    public NotificationRetryPolicy(
            int maxAttempts,
            Duration initialDelay,
            double multiplier,
            Duration maxDelay
    ) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException(
                    "maxAttempts must be greater than zero"
            );
        }
        this.initialDelay = positive(initialDelay, "initialDelay");
        if (multiplier < 1.0) {
            throw new IllegalArgumentException(
                    "multiplier must be greater than or equal to one"
            );
        }
        this.maxDelay = positive(maxDelay, "maxDelay");
        if (this.maxDelay.compareTo(this.initialDelay) < 0) {
            throw new IllegalArgumentException(
                    "maxDelay must be greater than or equal to initialDelay"
            );
        }
        this.maxAttempts = maxAttempts;
        this.multiplier = multiplier;
    }

    public static NotificationRetryPolicy defaults() {
        return new NotificationRetryPolicy(
                5,
                Duration.ofMinutes(1),
                2.0,
                Duration.ofMinutes(15)
        );
    }

    public boolean exhausted(int attemptCount) {
        return attemptCount >= maxAttempts;
    }

    public Instant nextAttemptAt(Instant failedAt, int attemptCount) {
        Objects.requireNonNull(failedAt, "failedAt is required");
        if (attemptCount < 1) {
            throw new IllegalArgumentException(
                    "attemptCount must be greater than zero"
            );
        }
        double factor = Math.pow(multiplier, attemptCount - 1);
        double candidateMillis = initialDelay.toMillis() * factor;
        long delayMillis = candidateMillis >= maxDelay.toMillis()
                ? maxDelay.toMillis()
                : (long) candidateMillis;
        return failedAt.plusMillis(delayMillis);
    }

    public int maxAttempts() {
        return maxAttempts;
    }

    private static Duration positive(Duration value, String name) {
        Objects.requireNonNull(value, name + " is required");
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }
}
