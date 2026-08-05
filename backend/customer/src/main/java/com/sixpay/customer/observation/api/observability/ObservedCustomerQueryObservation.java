package com.sixpay.customer.observation.api.observability;

import com.sixpay.customer.observation.application.exception
        .InvalidObservedCustomerCursorException;
import com.sixpay.customer.observation.application.exception
        .ObservedCustomerNotFoundException;
import com.sixpay.customer.observation.application.exception
        .ObservedCustomerQueryUnavailableException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public final class ObservedCustomerQueryObservation {

    public static final String REQUESTS =
            "sixpay.customer.observation.query.requests";

    public static final String DURATION =
            "sixpay.customer.observation.query.duration";

    public static final String RESULTS =
            "sixpay.customer.observation.query.results";

    public static final String FAILURES =
            "sixpay.customer.observation.query.failures";

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    ObservedCustomerQueryObservation.class
            );

    private final MeterRegistry meterRegistry;
    private final Clock clock;

    public ObservedCustomerQueryObservation(
            MeterRegistry meterRegistry,
            Clock clock
    ) {
        this.meterRegistry = Objects.requireNonNull(
                meterRegistry,
                "meterRegistry is required"
        );
        this.clock = Objects.requireNonNull(
                clock,
                "clock is required"
        );
    }

    public <T> T observe(
            ObservedCustomerQueryOperation operation,
            String correlationId,
            UUID observedCustomerId,
            Integer pageSize,
            Supplier<T> action,
            ResultMetadataExtractor<T> metadataExtractor
    ) {
        Objects.requireNonNull(
                operation,
                "operation is required"
        );
        Objects.requireNonNull(
                correlationId,
                "correlationId is required"
        );
        Objects.requireNonNull(
                action,
                "action is required"
        );
        Objects.requireNonNull(
                metadataExtractor,
                "metadataExtractor is required"
        );

        Instant startedAt = clock.instant();

        Counter.builder(REQUESTS)
                .tag("operation", operation.name())
                .register(meterRegistry)
                .increment();

        try (MDC.MDCCloseable ignored =
                     MDC.putCloseable(
                             "correlationId",
                             correlationId
                     )) {

            T value = action.get();
            ResultMetadata metadata =
                    metadataExtractor.extract(value);

            recordDuration(
                    operation,
                    ObservedCustomerQueryResult.SUCCESS,
                    startedAt
            );

            Counter.builder(RESULTS)
                    .tag("operation", operation.name())
                    .tag(
                            "result",
                            ObservedCustomerQueryResult
                                    .SUCCESS
                                    .name()
                    )
                    .register(meterRegistry)
                    .increment();

            LOGGER.info(
                    "Observed Customer query completed: "
                            + "operation={}, "
                            + "result={}, "
                            + "observedCustomerId={}, "
                            + "correlationId={}, "
                            + "durationMs={}, "
                            + "pageSize={}, "
                            + "hasMore={}",
                    operation,
                    ObservedCustomerQueryResult.SUCCESS,
                    observedCustomerId,
                    correlationId,
                    elapsedMillis(startedAt),
                    pageSize,
                    metadata.hasMore()
            );

            return value;
        } catch (RuntimeException exception) {
            Failure failure = classify(exception);

            recordDuration(
                    operation,
                    failure.result(),
                    startedAt
            );

            Counter.builder(FAILURES)
                    .tag("operation", operation.name())
                    .tag(
                            "result",
                            failure.result().name()
                    )
                    .tag(
                            "error_type",
                            failure.errorType().name()
                    )
                    .register(meterRegistry)
                    .increment();

            LOGGER.warn(
                    "Observed Customer query failed: "
                            + "operation={}, "
                            + "result={}, "
                            + "errorType={}, "
                            + "observedCustomerId={}, "
                            + "correlationId={}, "
                            + "durationMs={}, "
                            + "pageSize={}",
                    operation,
                    failure.result(),
                    failure.errorType(),
                    observedCustomerId,
                    correlationId,
                    elapsedMillis(startedAt),
                    pageSize
            );

            throw exception;
        }
    }

    private void recordDuration(
            ObservedCustomerQueryOperation operation,
            ObservedCustomerQueryResult result,
            Instant startedAt
    ) {
        Timer.builder(DURATION)
                .tag("operation", operation.name())
                .tag("result", result.name())
                .register(meterRegistry)
                .record(
                        Duration.between(
                                startedAt,
                                clock.instant()
                        )
                );
    }

    private long elapsedMillis(
            Instant startedAt
    ) {
        return Math.max(
                0,
                Duration.between(
                        startedAt,
                        clock.instant()
                ).toMillis()
        );
    }

    private static Failure classify(
            RuntimeException exception
    ) {
        if (exception
                instanceof InvalidObservedCustomerCursorException) {
            return new Failure(
                    ObservedCustomerQueryResult.INVALID,
                    ObservedCustomerQueryErrorType
                            .INVALID_CURSOR
            );
        }

        if (exception
                instanceof ObservedCustomerNotFoundException) {
            return new Failure(
                    ObservedCustomerQueryResult.NOT_FOUND,
                    ObservedCustomerQueryErrorType.NOT_FOUND
            );
        }

        if (exception
                instanceof ObservedCustomerQueryUnavailableException) {
            return new Failure(
                    ObservedCustomerQueryResult.UNAVAILABLE,
                    ObservedCustomerQueryErrorType
                            .TEMPORARY_UNAVAILABLE
            );
        }

        if (exception
                instanceof ObservedCustomerQueryRateLimitExceededException) {
            return new Failure(
                    ObservedCustomerQueryResult.RATE_LIMITED,
                    ObservedCustomerQueryErrorType.RATE_LIMIT
            );
        }

        if (exception instanceof IllegalArgumentException) {
            return new Failure(
                    ObservedCustomerQueryResult.INVALID,
                    ObservedCustomerQueryErrorType
                            .INVALID_FILTER
            );
        }

        return new Failure(
                ObservedCustomerQueryResult.INTERNAL_ERROR,
                ObservedCustomerQueryErrorType.INTERNAL
        );
    }

    public interface ResultMetadataExtractor<T> {

        ResultMetadata extract(T value);
    }

    public record ResultMetadata(
            boolean hasMore
    ) {

        public static ResultMetadata none() {
            return new ResultMetadata(false);
        }

        public static ResultMetadata page(
                boolean hasMore
        ) {
            return new ResultMetadata(hasMore);
        }
    }

    private record Failure(
            ObservedCustomerQueryResult result,
            ObservedCustomerQueryErrorType errorType
    ) {
    }
}
