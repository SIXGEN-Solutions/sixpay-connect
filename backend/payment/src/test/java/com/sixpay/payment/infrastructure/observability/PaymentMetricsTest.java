package com.sixpay.payment.infrastructure.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentMetricsTest {

    @Test
    void recordsLowCardinalityCounterAndTimer() {
        SimpleMeterRegistry registry =
                new SimpleMeterRegistry();

        PaymentMetrics metrics =
                new PaymentMetrics(registry);

        metrics.record(
                "PaymentReceptionService.receive",
                "success",
                Duration.ofMillis(25)
        );

        assertThat(
                registry.get(
                                PaymentMetrics
                                        .OPERATION_COUNTER
                        )
                        .tag(
                                "operation",
                                "PaymentReceptionService.receive"
                        )
                        .tag("outcome", "success")
                        .counter()
                        .count()
        ).isEqualTo(1.0);

        assertThat(
                registry.get(
                                PaymentMetrics
                                        .OPERATION_DURATION
                        )
                        .tag(
                                "operation",
                                "PaymentReceptionService.receive"
                        )
                        .tag("outcome", "success")
                        .timer()
                        .count()
        ).isOne();
    }
}
