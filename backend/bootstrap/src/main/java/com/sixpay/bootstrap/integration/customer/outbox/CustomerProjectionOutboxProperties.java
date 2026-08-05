package com.sixpay.bootstrap.integration.customer.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Objects;

@ConfigurationProperties(
        prefix = "sixpay.payment.outbox.customer-projection"
)
public record CustomerProjectionOutboxProperties(
        boolean enabled,
        int batchSize,
        Duration pollingInterval,
        int maxAttempts,
        Duration initialBackoff,
        Duration maxBackoff,
        Duration processingTimeout
) {

    private static final int MAX_BATCH_SIZE = 500;

    public CustomerProjectionOutboxProperties {
        if (batchSize < 1 || batchSize > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException(
                    "batchSize must be between 1 and " + MAX_BATCH_SIZE
            );
        }

        pollingInterval = requirePositive(
                pollingInterval,
                "pollingInterval"
        );

        if (maxAttempts < 1) {
            throw new IllegalArgumentException(
                    "maxAttempts must be at least one"
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

        processingTimeout = requirePositive(
                processingTimeout,
                "processingTimeout"
        );

        if (initialBackoff.compareTo(maxBackoff) > 0) {
            throw new IllegalArgumentException(
                    "initialBackoff must not exceed maxBackoff"
            );
        }

        if (pollingInterval.compareTo(processingTimeout) >= 0) {
            throw new IllegalArgumentException(
                    "pollingInterval must be shorter than processingTimeout"
            );
        }
    }

    private static Duration requirePositive(
            Duration value,
            String field
    ) {
        Objects.requireNonNull(value, field + " is required");

        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(
                    field + " must be positive"
            );
        }

        return value;
    }
}
