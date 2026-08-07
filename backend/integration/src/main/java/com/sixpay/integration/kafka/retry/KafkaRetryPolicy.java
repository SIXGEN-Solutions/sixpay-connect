package com.sixpay.integration.kafka.retry;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public record KafkaRetryPolicy(
        List<Duration> delays,
        int maximumAttempts
) {
    public KafkaRetryPolicy {
        delays = List.copyOf(
                Objects.requireNonNull(delays)
        );
        if (delays.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one retry delay is required"
            );
        }
        if (delays.stream().anyMatch(
                delay -> delay == null
                        || delay.isZero()
                        || delay.isNegative()
        )) {
            throw new IllegalArgumentException(
                    "Retry delays must be positive"
            );
        }
        if (maximumAttempts < 1) {
            throw new IllegalArgumentException(
                    "maximumAttempts must be >= 1"
            );
        }
    }
}
