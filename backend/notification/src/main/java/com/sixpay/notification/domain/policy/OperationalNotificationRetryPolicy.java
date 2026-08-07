package com.sixpay.notification.domain.policy;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class OperationalNotificationRetryPolicy {

    private final int maxAttempts;
    private final Duration initialBackoff;
    private final Duration maxBackoff;

    public OperationalNotificationRetryPolicy(
            int maxAttempts,
            Duration initialBackoff,
            Duration maxBackoff
    ) {
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException(
                    "maxAttempts must be positive"
            );
        }

        this.initialBackoff = positive(
                initialBackoff,
                "initialBackoff"
        );
        this.maxBackoff = positive(
                maxBackoff,
                "maxBackoff"
        );

        if (this.maxBackoff.compareTo(
                this.initialBackoff
        ) < 0) {
            throw new IllegalArgumentException(
                    "maxBackoff must be >= initialBackoff"
            );
        }

        this.maxAttempts = maxAttempts;
    }

    public boolean exhausted(
            int attemptCount
    ) {
        return attemptCount >= maxAttempts;
    }

    public Instant nextAttemptAt(
            Instant failedAt,
            int attemptCount
    ) {
        Objects.requireNonNull(
                failedAt,
                "failedAt"
        );

        if (attemptCount <= 0) {
            throw new IllegalArgumentException(
                    "attemptCount must be positive"
            );
        }

        long multiplier =
                1L << Math.min(
                        attemptCount - 1,
                        20
                );

        Duration delay =
                initialBackoff.multipliedBy(
                        multiplier
                );

        if (delay.compareTo(maxBackoff) > 0) {
            delay = maxBackoff;
        }

        return failedAt.plus(delay);
    }

    public int maxAttempts() {
        return maxAttempts;
    }

    private static Duration positive(
            Duration value,
            String name
    ) {
        if (value == null
                || value.isZero()
                || value.isNegative()) {
            throw new IllegalArgumentException(
                    name + " must be positive"
            );
        }

        return value;
    }
}
