package com.sixpay.notification.infrastructure.scheduling;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationRetrySchedulerTest {

    @Test
    void delegatesToRetryUseCase() {
        var calls = new AtomicInteger();
        var scheduler = new NotificationRetryScheduler(
                calls::incrementAndGet
        );

        scheduler.retryDueDeliveries();

        assertThat(calls).hasValue(1);
    }
}
