package com.sixpay.integration.resilience;

import java.time.Duration;
import java.util.Objects;

public record RetryPolicy(
        int maxAttempts,
        Duration initialBackoff,
        Duration maximumBackoff,
        double jitterRatio
) {
    public static final RetryPolicy DEFAULT =
            new RetryPolicy(3, Duration.ofMillis(250), Duration.ofSeconds(2), 0.20d);
    public RetryPolicy {
        if (maxAttempts < 1 || maxAttempts > 5) throw new IllegalArgumentException("maxAttempts must be 1..5");
        initialBackoff = positive(initialBackoff, "initialBackoff");
        maximumBackoff = positive(maximumBackoff, "maximumBackoff");
        if (maximumBackoff.compareTo(initialBackoff) < 0) throw new IllegalArgumentException("maximumBackoff must be >= initialBackoff");
        if (jitterRatio < 0 || jitterRatio > 1) throw new IllegalArgumentException("jitterRatio must be 0..1");
    }
    public Duration backoffForAttempt(int attempt) {
        if (attempt < 1) throw new IllegalArgumentException("attempt must be positive");
        Duration candidate = initialBackoff.multipliedBy(1L << Math.min(attempt - 1, 20));
        return candidate.compareTo(maximumBackoff) > 0 ? maximumBackoff : candidate;
    }
    private static Duration positive(Duration value, String name) {
        Objects.requireNonNull(value, name + " is required");
        if (value.isZero() || value.isNegative()) throw new IllegalArgumentException(name + " must be positive");
        return value;
    }
}
