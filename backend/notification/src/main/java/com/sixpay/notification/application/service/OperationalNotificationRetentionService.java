package com.sixpay.notification.application.service;

import com.sixpay.notification.application.port.input.OperationalNotificationPurgeReport;
import com.sixpay.notification.application.port.input.OperationalNotificationRetentionUseCase;
import com.sixpay.notification.application.port.output.OperationalNotificationOperationsTelemetry;
import com.sixpay.notification.domain.repository.OperationalNotificationOperationsRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class OperationalNotificationRetentionService
        implements OperationalNotificationRetentionUseCase {

    private final OperationalNotificationOperationsRepository repository;
    private final Duration deliveredRetention;
    private final Duration failedRetention;
    private final int batchSize;
    private final OperationalNotificationOperationsTelemetry telemetry;

    public OperationalNotificationRetentionService(
            OperationalNotificationOperationsRepository repository,
            Duration deliveredRetention,
            Duration failedRetention,
            int batchSize,
            OperationalNotificationOperationsTelemetry telemetry
    ) {
        this.repository = Objects.requireNonNull(
                repository,
                "repository"
        );
        this.deliveredRetention = positive(
                deliveredRetention,
                "deliveredRetention"
        );
        this.failedRetention = positive(
                failedRetention,
                "failedRetention"
        );

        if (batchSize <= 0) {
            throw new IllegalArgumentException(
                    "batchSize must be positive"
            );
        }

        this.batchSize = batchSize;
        this.telemetry = Objects.requireNonNull(
                telemetry,
                "telemetry"
        );
    }

    @Override
    public OperationalNotificationPurgeReport purge(
            Instant now
    ) {
        Objects.requireNonNull(
                now,
                "now"
        );

        int deleted = repository.purgeTerminal(
                now.minus(deliveredRetention),
                now.minus(failedRetention),
                batchSize
        );

        if (deleted > 0) {
            telemetry.recordPurged(
                    deleted
            );
        }

        return new OperationalNotificationPurgeReport(
                deleted,
                now
        );
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
