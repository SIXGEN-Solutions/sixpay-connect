package com.sixpay.notification.infrastructure.operational.observability;

import com.sixpay.notification.application.port.output.OperationalNotificationOperationsTelemetry;
import com.sixpay.notification.domain.model.NotificationDeliveryStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public final class OperationalNotificationMetrics
        implements OperationalNotificationOperationsTelemetry {

    private static final String PREFIX =
            "sixpay.notification.operational";

    private final Map<
            NotificationDeliveryStatus,
            AtomicLong
            > statusCounts =
            new EnumMap<>(
                    NotificationDeliveryStatus.class
            );

    private final AtomicLong dueCount =
            new AtomicLong();

    private final AtomicLong oldestDueAgeSeconds =
            new AtomicLong();

    private final Counter replayCounter;
    private final Counter purgedCounter;

    public OperationalNotificationMetrics(
            MeterRegistry registry
    ) {
        for (NotificationDeliveryStatus status
                : NotificationDeliveryStatus.values()) {
            AtomicLong value = new AtomicLong();
            statusCounts.put(status, value);

            Gauge.builder(
                            PREFIX + ".status",
                            value,
                            AtomicLong::doubleValue
                    )
                    .tag(
                            "status",
                            status.name()
                    )
                    .description(
                            "Operational notifications by canonical status"
                    )
                    .register(registry);
        }

        Gauge.builder(
                        PREFIX + ".due",
                        dueCount,
                        AtomicLong::doubleValue
                )
                .description(
                        "Operational notifications currently due for delivery"
                )
                .register(registry);

        Gauge.builder(
                        PREFIX + ".oldest.due.age.seconds",
                        oldestDueAgeSeconds,
                        AtomicLong::doubleValue
                )
                .description(
                        "Age input seconds of the oldest due notification"
                )
                .register(registry);

        replayCounter = Counter.builder(
                        PREFIX + ".replay.total"
                )
                .description(
                        "Controlled operational notification replays"
                )
                .register(registry);

        purgedCounter = Counter.builder(
                        PREFIX + ".purged.total"
                )
                .description(
                        "Purged terminal operational notifications"
                )
                .register(registry);
    }

    public void refreshStatus(
            NotificationDeliveryStatus status,
            long count
    ) {
        statusCounts.get(status)
                .set(Math.max(0L, count));
    }

    public void refreshDue(
            long count,
            long ageSeconds
    ) {
        dueCount.set(
                Math.max(0L, count)
        );
        oldestDueAgeSeconds.set(
                Math.max(0L, ageSeconds)
        );
    }

    @Override
    public void recordReplay() {
        replayCounter.increment();
    }

    @Override
    public void recordPurged(
            int count
    ) {
        if (count > 0) {
            purgedCounter.increment(count);
        }
    }
}
