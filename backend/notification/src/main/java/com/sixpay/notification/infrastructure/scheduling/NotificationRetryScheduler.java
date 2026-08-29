package com.sixpay.notification.infrastructure.scheduling;

import com.sixpay.notification.application.port.input.RetryNotificationDeliveriesUseCase;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Objects;

public final class NotificationRetryScheduler {

    private final RetryNotificationDeliveriesUseCase retryUseCase;

    public NotificationRetryScheduler(
            RetryNotificationDeliveriesUseCase retryUseCase
    ) {
        this.retryUseCase = Objects.requireNonNull(retryUseCase);
    }

    @Scheduled(
            fixedDelayString =
                    "${sixpay.notification.retry.scheduler-delay:10s}"
    )
    public void retryDueDeliveries() {
        retryUseCase.retryDueDeliveries();
    }
}
