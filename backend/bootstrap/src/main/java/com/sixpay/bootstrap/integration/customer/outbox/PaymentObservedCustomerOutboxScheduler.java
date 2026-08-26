package com.sixpay.bootstrap.integration.customer.outbox;

import com.sixpay.payment.infrastructure.outbox
        .PaymentOutboxRepository;
import com.sixpay.payment.infrastructure.outbox.serialization
        .PaymentOutboxEventTypeRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class PaymentObservedCustomerOutboxScheduler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    PaymentObservedCustomerOutboxScheduler.class
            );

    private static final String EVENT_TYPE =
            PaymentOutboxEventTypeRegistry
                    .OBSERVED_CUSTOMER_PROJECTION_TYPE;

    private final PaymentObservedCustomerOutboxDispatcher dispatcher;
    private final CustomerProjectionOutboxProperties properties;
    private final CustomerProjectionOutboxMetrics metrics;
    private final PaymentOutboxRepository repository;
    private final Clock clock;
    private final String workerId;

    public PaymentObservedCustomerOutboxScheduler(
            PaymentObservedCustomerOutboxDispatcher dispatcher,
            CustomerProjectionOutboxProperties properties,
            CustomerProjectionOutboxMetrics metrics,
            PaymentOutboxRepository repository,
            Clock clock
    ) {
        this.dispatcher = Objects.requireNonNull(
                dispatcher,
                "dispatcher is required"
        );
        this.properties = Objects.requireNonNull(
                properties,
                "properties is required"
        );
        this.metrics = Objects.requireNonNull(
                metrics,
                "metrics is required"
        );
        this.repository = Objects.requireNonNull(
                repository,
                "repository is required"
        );
        this.clock = Objects.requireNonNull(
                clock,
                "clock is required"
        );
        workerId = "customer-projection-" + UUID.randomUUID();
    }

    @Scheduled(
            fixedDelayString =
                    "${sixpay.payment.outbox.customer-projection."
                            + "polling-interval}"
    )
    public void dispatchAvailable() {
        Instant startedAt = clock.instant();
        Timer.Sample sample = metrics.start();

        try {
            List<PaymentObservedCustomerOutboxResult> results =
                    dispatcher.dispatchAvailable(
                            startedAt,
                            workerId,
                            properties.batchSize(),
                            properties.processingTimeout(),
                            properties.maxAttempts(),
                            properties.initialBackoff(),
                            properties.maxBackoff()
                    );

            metrics.recordBatch(results, sample);
            updateLag(startedAt);

            long durationMs = Duration.between(
                    startedAt,
                    clock.instant()
            ).toMillis();

            LOGGER.info(
                    "Customer projection outbox dispatch completed: "
                            + "eventType={}, result={}, count={}, "
                            + "durationMs={}",
                    EVENT_TYPE,
                    batchResult(results),
                    results.size(),
                    durationMs
            );
        } catch (RuntimeException exception) {
            metrics.recordSchedulerFailure(sample);

            long durationMs = Duration.between(
                    startedAt,
                    clock.instant()
            ).toMillis();

            LOGGER.warn(
                    "Customer projection outbox dispatch failed: "
                            + "eventType={}, result=failed, "
                            + "durationMs={}",
                    EVENT_TYPE,
                    durationMs,
                    exception
            );
        }
    }

    private void updateLag(Instant now) {
        Duration lag = repository
                .findOldestOutstandingOccurredAt(EVENT_TYPE)
                .map(oldest ->
                        oldest.isAfter(now)
                                ? Duration.ZERO
                                : Duration.between(oldest, now)
                )
                .orElse(Duration.ZERO);

        metrics.updateLag(lag);
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
}
