package com.sixpay.notification.infrastructure.operational.observability;

import com.sixpay.notification.domain.model.NotificationDeliveryStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OperationalNotificationMetricsTest {

    @Test
    void exposesLowCardinalityOperationalMetrics() {
        SimpleMeterRegistry registry =
                new SimpleMeterRegistry();

        OperationalNotificationMetrics metrics =
                new OperationalNotificationMetrics(
                        registry
                );

        metrics.refreshStatus(
                NotificationDeliveryStatus.DEAD_LETTERED,
                3
        );
        metrics.refreshDue(
                4,
                120
        );
        metrics.recordReplay();
        metrics.recordPurged(5);

        assertEquals(
                3.0,
                registry.get(
                                "sixpay.notification.operational.status"
                        )
                        .tag(
                                "status",
                                "DEAD_LETTERED"
                        )
                        .gauge()
                        .value()
        );

        assertEquals(
                4.0,
                registry.get(
                                "sixpay.notification.operational.due"
                        )
                        .gauge()
                        .value()
        );

        assertEquals(
                120.0,
                registry.get(
                                "sixpay.notification.operational.oldest.due.age.seconds"
                        )
                        .gauge()
                        .value()
        );

        assertEquals(
                1.0,
                registry.get(
                                "sixpay.notification.operational.replay.total"
                        )
                        .counter()
                        .count()
        );

        assertEquals(
                5.0,
                registry.get(
                                "sixpay.notification.operational.purged.total"
                        )
                        .counter()
                        .count()
        );
    }
}
