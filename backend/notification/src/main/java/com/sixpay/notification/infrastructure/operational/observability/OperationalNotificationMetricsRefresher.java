package com.sixpay.notification.infrastructure.operational.observability;

import com.sixpay.notification.domain.model.NotificationDeliveryStatus;
import com.sixpay.notification.domain.repository.OperationalNotificationOperationsRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class OperationalNotificationMetricsRefresher {

    private final OperationalNotificationOperationsRepository repository;
    private final OperationalNotificationMetrics metrics;
    private final Clock clock;

    public OperationalNotificationMetricsRefresher(
            OperationalNotificationOperationsRepository repository,
            OperationalNotificationMetrics metrics,
            Clock clock
    ) {
        this.repository = Objects.requireNonNull(
                repository,
                "repository"
        );
        this.metrics = Objects.requireNonNull(
                metrics,
                "metrics"
        );
        this.clock = Objects.requireNonNull(
                clock,
                "clock"
        );
    }

    public void refresh() {
        Instant now = clock.instant();

        for (NotificationDeliveryStatus status
                : NotificationDeliveryStatus.values()) {
            metrics.refreshStatus(
                    status,
                    repository.countByStatus(
                            status
                    )
            );
        }

        long due = repository.countDue(
                now
        );

        long oldestAge =
                repository.findOldestDueAt(now)
                        .map(oldest ->
                                Math.max(
                                        0L,
                                        Duration.between(
                                                oldest,
                                                now
                                        ).toSeconds()
                                )
                        )
                        .orElse(0L);

        metrics.refreshDue(
                due,
                oldestAge
        );
    }
}
