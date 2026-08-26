package com.sixpay.notification.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(
        prefix = OperationalNotificationRetryProperties.PREFIX
)
public record OperationalNotificationRetryProperties(
        boolean enabled,
        int maxAttempts,
        Duration initialBackoff,
        Duration maxBackoff,
        int batchSize,
        long pollIntervalMs
) {
    public static final String PREFIX =
            "sixpay.notification.operational.retry";

    public OperationalNotificationRetryProperties {
        if (maxAttempts <= 0) {
            maxAttempts = 5;
        }

        if (initialBackoff == null
                || initialBackoff.isZero()
                || initialBackoff.isNegative()) {
            initialBackoff = Duration.ofSeconds(30);
        }

        if (maxBackoff == null
                || maxBackoff.isZero()
                || maxBackoff.isNegative()) {
            maxBackoff = Duration.ofMinutes(15);
        }

        if (maxBackoff.compareTo(initialBackoff) < 0) {
            throw new IllegalArgumentException(
                    "maxBackoff must be >= initialBackoff"
            );
        }

        if (batchSize <= 0) {
            batchSize = 50;
        }

        if (pollIntervalMs <= 0) {
            pollIntervalMs = 30_000L;
        }
    }
}
