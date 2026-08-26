package com.sixpay.bootstrap.integration.customer.outbox;

import com.sixpay.payment.infrastructure.outbox.serialization
        .PaymentOutboxEventTypeRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public final class CustomerProjectionOutboxMetrics {

    private static final String PREFIX =
            "sixpay.payment.outbox.customer_projection";

    private static final String EVENT_TYPE =
            PaymentOutboxEventTypeRegistry
                    .OBSERVED_CUSTOMER_PROJECTION_TYPE;

    private final Counter claimed;
    private final Counter published;
    private final MeterRegistry registry;
    private final AtomicLong lagSeconds =
            new AtomicLong();

    public CustomerProjectionOutboxMetrics(
            MeterRegistry registry
    ) {
        this.registry = Objects.requireNonNull(
                registry,
                "registry is required"
        );

        claimed = Counter.builder(PREFIX + ".claimed")
                .tag("event_type", EVENT_TYPE)
                .register(registry);

        published = Counter.builder(PREFIX + ".published")
                .tag("event_type", EVENT_TYPE)
                .register(registry);

        registry.gauge(
                PREFIX + ".lag",
                List.of(
                        Tag.of("event_type", EVENT_TYPE)
                ),
                lagSeconds,
                AtomicLong::doubleValue
        );
    }

    public Timer.Sample start() {
        return Timer.start(registry);
    }

    public void recordBatch(
            List<PaymentObservedCustomerOutboxResult> results,
            Timer.Sample sample
    ) {
        Objects.requireNonNull(results, "results is required");
        Objects.requireNonNull(sample, "sample is required");

        claimed.increment(results.size());

        for (var result : results) {
            switch (result.outcome()) {
                case PUBLISHED -> published.increment();
                case RETRY_SCHEDULED ->
                        retryCounter(
                                normalizeErrorType(
                                        result.errorType()
                                )
                        ).increment();
                case DEAD_LETTERED ->
                        deadCounter(
                                normalizeErrorType(
                                        result.errorType()
                                )
                        ).increment();
            }
        }

        sample.stop(
                Timer.builder(PREFIX + ".duration")
                        .tag("event_type", EVENT_TYPE)
                        .tag("result", batchResult(results))
                        .register(registry)
        );
    }

    public void recordSchedulerFailure(
            Timer.Sample sample
    ) {
        Objects.requireNonNull(sample, "sample is required");

        sample.stop(
                Timer.builder(PREFIX + ".duration")
                        .tag("event_type", EVENT_TYPE)
                        .tag("result", "failed")
                        .register(registry)
        );
    }

    public void updateLag(Duration lag) {
        Objects.requireNonNull(lag, "lag is required");
        lagSeconds.set(
                Math.max(0L, lag.toSeconds())
        );
    }

    private Counter retryCounter(String errorType) {
        return Counter.builder(PREFIX + ".retried")
                .tag("event_type", EVENT_TYPE)
                .tag("error_type", errorType)
                .register(registry);
    }

    private Counter deadCounter(String errorType) {
        return Counter.builder(PREFIX + ".dead_lettered")
                .tag("event_type", EVENT_TYPE)
                .tag("error_type", errorType)
                .register(registry);
    }

    private static String batchResult(
            List<PaymentObservedCustomerOutboxResult> results
    ) {
        if (results.isEmpty()) {
            return "empty";
        }

        if (results.stream().anyMatch(result ->
                result.outcome()
                        == PaymentObservedCustomerOutboxResult
                        .Outcome.DEAD_LETTERED
        )) {
            return "dead_lettered";
        }

        if (results.stream().anyMatch(result ->
                result.outcome()
                        == PaymentObservedCustomerOutboxResult
                        .Outcome.RETRY_SCHEDULED
        )) {
            return "retried";
        }

        return "published";
    }

    private static String normalizeErrorType(
            String errorType
    ) {
        if (errorType == null || errorType.isBlank()) {
            return "none";
        }

        return switch (errorType) {
            case "unknown_event_type",
                 "unsupported_event_version",
                 "invalid_event_payload",
                 "projection_domain_conflict",
                 "invalid_contract",
                 "temporary_persistence_failure",
                 "temporary_infrastructure_failure" ->
                    errorType;
            default -> "other";
        };
    }
}
