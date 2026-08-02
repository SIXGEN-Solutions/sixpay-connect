package com.sixpay.payment.infrastructure.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;

/**
 * Low-cardinality Payment metrics.
 *
 * <p>No Payment ID, external reference, account reference, customer ID or
 * Partner subject is ever used as a metric tag.</p>
 */
@Component
@ConditionalOnBean(MeterRegistry.class)
public final class PaymentMetrics {

    static final String OPERATION_COUNTER =
            "sixpay.payment.operations";
    static final String OPERATION_DURATION =
            "sixpay.payment.operation.duration";

    private final MeterRegistry registry;

    public PaymentMetrics(MeterRegistry registry) {
        this.registry = Objects.requireNonNull(
                registry,
                "Meter registry"
        );
    }

    public void record(
            String operation,
            String outcome,
            Duration duration
    ) {
        String safeOperation = requireTag(
                operation,
                "operation"
        );
        String safeOutcome = requireTag(
                outcome,
                "outcome"
        );
        Duration safeDuration = Objects.requireNonNull(
                duration,
                "Operation duration"
        );

        Counter.builder(OPERATION_COUNTER)
                .description(
                        "Number of Payment application operations"
                )
                .tag("operation", safeOperation)
                .tag("outcome", safeOutcome)
                .register(registry)
                .increment();

        Timer.builder(OPERATION_DURATION)
                .description(
                        "Duration of Payment application operations"
                )
                .tag("operation", safeOperation)
                .tag("outcome", safeOutcome)
                .publishPercentileHistogram()
                .register(registry)
                .record(safeDuration);
    }

    private static String requireTag(
            String value,
            String label
    ) {
        if (value == null
                || value.isBlank()
                || value.length() > 100) {
            throw new IllegalArgumentException(
                    label + " metric tag is invalid"
            );
        }
        return value;
    }
}
