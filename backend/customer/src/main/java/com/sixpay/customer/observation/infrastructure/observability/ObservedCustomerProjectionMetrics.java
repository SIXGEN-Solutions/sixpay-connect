package com.sixpay.customer.observation.infrastructure.observability;

import com.sixpay.customer.observation.application.port.input
        .ObserveCustomerCommand;
import com.sixpay.customer.observation.application.port.input
        .ObserveCustomerResult;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;
import java.util.Objects;

/**
 * Owns all projection metric names and bounded tag construction.
 */
public final class ObservedCustomerProjectionMetrics {

    public static final String REQUESTS =
            "sixpay.customer.observation.projection.requests";
    public static final String DURATION =
            "sixpay.customer.observation.projection.duration";
    public static final String RESULTS =
            "sixpay.customer.observation.projection.results";
    public static final String FAILURES =
            "sixpay.customer.observation.projection.failures";
    public static final String RETRIES =
            "sixpay.customer.observation.projection.retries";
    public static final String REPLAYS =
            "sixpay.customer.observation.projection.replays";
    public static final String STALE =
            "sixpay.customer.observation.projection.stale";
    public static final String LAG =
            "sixpay.customer.observation.projection.lag";

    private final MeterRegistry meterRegistry;
    private final ThreadLocal<Integer> currentAttempt =
            ThreadLocal.withInitial(() -> 1);

    public ObservedCustomerProjectionMetrics(
            MeterRegistry meterRegistry
    ) {
        this.meterRegistry = Objects.requireNonNull(
                meterRegistry,
                "meterRegistry is required"
        );
    }

    public void begin(ObserveCustomerCommand command) {
        Objects.requireNonNull(command, "command is required");
        currentAttempt.set(1);

        Counter.builder(REQUESTS)
                .tag("event_type", eventType(command))
                .register(meterRegistry)
                .increment();
    }

    public void attempt(int attempt) {
        if (attempt < 1) {
            throw new IllegalArgumentException(
                    "attempt must be positive"
            );
        }
        currentAttempt.set(attempt);
    }

    public int currentAttempt() {
        return currentAttempt.get();
    }

    public void retry(
            ObserveCustomerCommand command,
            int failedAttempt,
            ObservedCustomerProjectionErrorType errorType
    ) {
        Objects.requireNonNull(command, "command is required");
        Objects.requireNonNull(errorType, "errorType is required");

        Counter.builder(RETRIES)
                .tag("event_type", eventType(command))
                .tag("error_type", errorType.name())
                .tag(
                        "attempt_bucket",
                        attemptBucket(failedAttempt)
                )
                .register(meterRegistry)
                .increment();
    }

    public void success(
            ObserveCustomerCommand command,
            ObserveCustomerResult result,
            Duration duration,
            Duration lag
    ) {
        Objects.requireNonNull(command, "command is required");
        Objects.requireNonNull(result, "result is required");

        ObservedCustomerProjectionResultType resultType =
                resultType(result);

        Counter.builder(RESULTS)
                .tag("event_type", eventType(command))
                .tag("result", resultType.name())
                .register(meterRegistry)
                .increment();

        if (resultType
                == ObservedCustomerProjectionResultType.REPLAYED) {
            Counter.builder(REPLAYS)
                    .tag("event_type", eventType(command))
                    .register(meterRegistry)
                    .increment();
        }

        if (resultType
                == ObservedCustomerProjectionResultType
                .IGNORED_STALE) {
            Counter.builder(STALE)
                    .tag("event_type", eventType(command))
                    .register(meterRegistry)
                    .increment();
        }

        recordDuration(
                command,
                resultType,
                duration
        );
        recordLag(
                command,
                resultType,
                lag
        );
    }

    public void failure(
            ObserveCustomerCommand command,
            ObservedCustomerProjectionErrorType errorType,
            Duration duration,
            Duration lag
    ) {
        Objects.requireNonNull(command, "command is required");
        Objects.requireNonNull(errorType, "errorType is required");

        Counter.builder(FAILURES)
                .tag("event_type", eventType(command))
                .tag(
                        "result",
                        ObservedCustomerProjectionResultType
                                .FAILED
                                .name()
                )
                .tag("error_type", errorType.name())
                .register(meterRegistry)
                .increment();

        recordDuration(
                command,
                ObservedCustomerProjectionResultType.FAILED,
                duration
        );
        recordLag(
                command,
                ObservedCustomerProjectionResultType.FAILED,
                lag
        );
    }

    public void clearAttempt() {
        currentAttempt.remove();
    }

    public static String eventType(
            ObserveCustomerCommand command
    ) {
        return "PAYMENT_" + command.paymentStatus().name();
    }

    public static String attemptBucket(int attempt) {
        if (attempt <= 1) {
            return "1";
        }
        if (attempt == 2) {
            return "2";
        }
        return "3_PLUS";
    }

    public static ObservedCustomerProjectionResultType resultType(
            ObserveCustomerResult result
    ) {
        return switch (result.disposition()) {
            case APPLIED ->
                    ObservedCustomerProjectionResultType.APPLIED;
            case REPLAYED ->
                    ObservedCustomerProjectionResultType.REPLAYED;
            case IGNORED_STALE ->
                    ObservedCustomerProjectionResultType
                            .IGNORED_STALE;
        };
    }

    private void recordDuration(
            ObserveCustomerCommand command,
            ObservedCustomerProjectionResultType result,
            Duration duration
    ) {
        Timer.builder(DURATION)
                .tag("event_type", eventType(command))
                .tag("result", result.name())
                .register(meterRegistry)
                .record(nonNegative(duration));
    }

    private void recordLag(
            ObserveCustomerCommand command,
            ObservedCustomerProjectionResultType result,
            Duration lag
    ) {
        Timer.builder(LAG)
                .tag("event_type", eventType(command))
                .tag("result", result.name())
                .register(meterRegistry)
                .record(nonNegative(lag));
    }

    private static Duration nonNegative(Duration value) {
        if (value == null || value.isNegative()) {
            return Duration.ZERO;
        }
        return value;
    }
}
