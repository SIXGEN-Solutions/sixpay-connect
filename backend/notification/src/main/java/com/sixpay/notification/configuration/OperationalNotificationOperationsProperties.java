package com.sixpay.notification.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(
        prefix = OperationalNotificationOperationsProperties.PREFIX
)
public record OperationalNotificationOperationsProperties(
        boolean metricsEnabled,
        long metricsRefreshMs,
        boolean retentionEnabled,
        Duration deliveredRetention,
        Duration failedRetention,
        int purgeBatchSize,
        long purgeIntervalMs
) {
    public static final String PREFIX =
            "sixpay.notification.operational.operations";

    public OperationalNotificationOperationsProperties {
        if (metricsRefreshMs <= 0) {
            metricsRefreshMs = 30_000L;
        }

        if (deliveredRetention == null
                || deliveredRetention.isZero()
                || deliveredRetention.isNegative()) {
            deliveredRetention = Duration.ofDays(90);
        }

        if (failedRetention == null
                || failedRetention.isZero()
                || failedRetention.isNegative()) {
            failedRetention = Duration.ofDays(365);
        }

        if (purgeBatchSize <= 0) {
            purgeBatchSize = 500;
        }

        if (purgeIntervalMs <= 0) {
            purgeIntervalMs = 86_400_000L;
        }
    }
}
