package com.sixpay.notification.infrastructure.operational.scheduling;

import com.sixpay.notification.application.port.input.OperationalNotificationRetentionUseCase;

import java.time.Clock;
import java.util.Objects;

public final class OperationalNotificationRetentionScheduler {

    private final OperationalNotificationRetentionUseCase retention;
    private final Clock clock;

    public OperationalNotificationRetentionScheduler(
            OperationalNotificationRetentionUseCase retention,
            Clock clock
    ) {
        this.retention = Objects.requireNonNull(
                retention,
                "retention"
        );
        this.clock = Objects.requireNonNull(
                clock,
                "clock"
        );
    }

    public void runOnce() {
        retention.purge(
                clock.instant()
        );
    }
}
