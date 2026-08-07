package com.sixpay.notification.infrastructure.operational.scheduling;

import com.sixpay.notification.application.port.input.ProcessOperationalNotificationsUseCase;
import com.sixpay.notification.configuration.OperationalNotificationRetryProperties;

import java.time.Clock;

public final class OperationalNotificationRetryScheduler {

    private final ProcessOperationalNotificationsUseCase processor;
    private final OperationalNotificationRetryProperties properties;
    private final Clock clock;

    public OperationalNotificationRetryScheduler(
            ProcessOperationalNotificationsUseCase processor,
            OperationalNotificationRetryProperties properties,
            Clock clock
    ) {
        this.processor = processor;
        this.properties = properties;
        this.clock = clock;
    }

    public void runOnce() {
        processor.processDue(
                clock.instant(),
                properties.batchSize()
        );
    }
}
