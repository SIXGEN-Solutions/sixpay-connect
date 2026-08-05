package com.sixpay.customer.observation.configuration;

import org.springframework.boot.context.properties
        .ConfigurationProperties;
import org.springframework.boot.context.properties.bind
        .DefaultValue;

import java.time.Duration;
import java.util.Objects;

@ConfigurationProperties(
        prefix = "sixpay.customer.observation.resilience"
)
public record ObservedCustomerProjectionResilienceProperties(
        @DefaultValue("3")
        int maxAttempts,
        @DefaultValue("10ms")
        Duration initialBackoff,
        @DefaultValue("250ms")
        Duration maxBackoff,
        @DefaultValue("2")
        double multiplier,
        @DefaultValue("0.20")
        double jitter
) {

    public ObservedCustomerProjectionResilienceProperties {
        if (maxAttempts < 1 || maxAttempts > 10) {
            throw new IllegalArgumentException(
                    "maxAttempts must be between 1 and 10"
            );
        }

        initialBackoff = requirePositive(
                initialBackoff,
                "initialBackoff"
        );
        maxBackoff = requirePositive(
                maxBackoff,
                "maxBackoff"
        );

        if (maxBackoff.compareTo(initialBackoff) < 0) {
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
    }

    private static Duration requirePositive(
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
}
