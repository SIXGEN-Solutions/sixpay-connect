package com.sixpay.bootstrap.integration.customer.outbox;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CustomerProjectionOutboxMetricsTest {

    @Test
    void recordsOnlyBoundedMetricTags() {
        SimpleMeterRegistry registry =
                new SimpleMeterRegistry();

        CustomerProjectionOutboxMetrics metrics =
                new CustomerProjectionOutboxMetrics(
                        registry
                );

        var sample = metrics.start();

        metrics.recordBatch(
                List.of(
                        result(
                                PaymentObservedCustomerOutboxResult
                                        .Outcome.PUBLISHED,
                                null
                        ),
                        result(
                                PaymentObservedCustomerOutboxResult
                                        .Outcome.RETRY_SCHEDULED,
                                "temporary_persistence_failure"
                        ),
                        result(
                                PaymentObservedCustomerOutboxResult
                                        .Outcome.DEAD_LETTERED,
                                "unknown_event_type"
                        )
                ),
                sample
        );

        metrics.updateLag(Duration.ofSeconds(42));

        assertEquals(
                3.0,
                registry.get(
                                "sixpay.payment.outbox."
                                        + "customer_projection.claimed"
                        )
                        .counter()
                        .count()
        );

        assertEquals(
                1.0,
                registry.get(
                                "sixpay.payment.outbox."
                                        + "customer_projection.published"
                        )
                        .counter()
                        .count()
        );

        assertEquals(
                1.0,
                registry.get(
                                "sixpay.payment.outbox."
                                        + "customer_projection.retried"
                        )
                        .tag(
                                "error_type",
                                "temporary_persistence_failure"
                        )
                        .counter()
                        .count()
        );

        assertEquals(
                1.0,
                registry.get(
                                "sixpay.payment.outbox."
                                        + "customer_projection."
                                        + "dead_lettered"
                        )
                        .tag(
                                "error_type",
                                "unknown_event_type"
                        )
                        .counter()
                        .count()
        );

        assertNotNull(
                registry.get(
                                "sixpay.payment.outbox."
                                        + "customer_projection.duration"
                        )
                        .tag("result", "dead_lettered")
                        .timer()
        );

        assertEquals(
                42.0,
                registry.get(
                                "sixpay.payment.outbox."
                                        + "customer_projection.lag"
                        )
                        .gauge()
                        .value()
        );
    }

    private static PaymentObservedCustomerOutboxResult result(
            PaymentObservedCustomerOutboxResult.Outcome outcome,
            String errorType
    ) {
        return new PaymentObservedCustomerOutboxResult(
                UUID.randomUUID(),
                outcome,
                1,
                errorType,
                outcome
                        == PaymentObservedCustomerOutboxResult
                        .Outcome.RETRY_SCHEDULED
                        ? Instant.parse(
                                "2026-08-04T21:00:00Z"
                        )
                        : null
        );
    }
}
