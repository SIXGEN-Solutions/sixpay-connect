package com.sixpay.customer.observation.infrastructure.observability;

import com.sixpay.customer.observation.application.port.input
        .ObserveCustomerCommand;
import com.sixpay.customer.observation.application.port.input
        .ObserveCustomerResult;
import com.sixpay.customer.observation.application.port.input
        .ObserveCustomerUseCase;
import com.sixpay.customer.observation.domain.exception
        .ObservedCustomerDomainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.TransactionException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Outermost projection decorator responsible for metrics and safe logs.
 */
public final class ObservedCustomerProjectionObservation
        implements ObserveCustomerUseCase {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    ObservedCustomerProjectionObservation.class
            );

    private final ObserveCustomerUseCase delegate;
    private final ObservedCustomerProjectionMetrics metrics;
    private final Clock clock;

    public ObservedCustomerProjectionObservation(
            ObserveCustomerUseCase delegate,
            ObservedCustomerProjectionMetrics metrics,
            Clock clock
    ) {
        this.delegate = Objects.requireNonNull(
                delegate,
                "delegate is required"
        );
        this.metrics = Objects.requireNonNull(
                metrics,
                "metrics is required"
        );
        this.clock = Objects.requireNonNull(
                clock,
                "clock is required"
        );
    }

    @Override
    public ObserveCustomerResult observe(
            ObserveCustomerCommand command
    ) {
        Objects.requireNonNull(command, "command is required");

        Instant startedAt = clock.instant();
        metrics.begin(command);

        try {
            ObserveCustomerResult result =
                    delegate.observe(command);

            Instant completedAt = clock.instant();
            Duration duration = between(
                    startedAt,
                    completedAt
            );
            Duration lag = lag(
                    command.observedAt(),
                    completedAt
            );
            int attempt = metrics.currentAttempt();

            metrics.success(
                    command,
                    result,
                    duration,
                    lag
            );

            LOGGER.info(
                    "Observed Customer projection completed: "
                            + "sourceEventId={}, paymentId={}, "
                            + "observedCustomerId={}, "
                            + "correlationId={}, result={}, "
                            + "attempt={}, durationMs={}, lagMs={}",
                    command.sourceEventId(),
                    command.paymentId(),
                    result.observedCustomerId().value(),
                    command.correlationId(),
                    ObservedCustomerProjectionMetrics
                            .resultType(result),
                    attempt,
                    duration.toMillis(),
                    lag.toMillis()
            );

            return result;
        } catch (RuntimeException exception) {
            Instant failedAt = clock.instant();
            Duration duration = between(
                    startedAt,
                    failedAt
            );
            Duration lag = lag(
                    command.observedAt(),
                    failedAt
            );
            int attempt = metrics.currentAttempt();
            ObservedCustomerProjectionErrorType errorType =
                    classify(exception);

            metrics.failure(
                    command,
                    errorType,
                    duration,
                    lag
            );

            LOGGER.warn(
                    "Observed Customer projection failed: "
                            + "sourceEventId={}, paymentId={}, "
                            + "observedCustomerId={}, "
                            + "correlationId={}, result={}, "
                            + "attempt={}, durationMs={}, lagMs={}",
                    command.sourceEventId(),
                    command.paymentId(),
                    null,
                    command.correlationId(),
                    ObservedCustomerProjectionResultType.FAILED,
                    attempt,
                    duration.toMillis(),
                    lag.toMillis()
            );

            throw exception;
        } finally {
            metrics.clearAttempt();
        }
    }

    public static ObservedCustomerProjectionErrorType classify(
            RuntimeException exception
    ) {
        if (exception
                instanceof ObjectOptimisticLockingFailureException) {
            return ObservedCustomerProjectionErrorType
                    .OPTIMISTIC_LOCK;
        }

        if (exception
                instanceof DataIntegrityViolationException) {
            return ObservedCustomerProjectionErrorType
                    .DATA_INTEGRITY;
        }

        if (exception
                instanceof ObservedCustomerDomainException) {
            return ObservedCustomerProjectionErrorType.DOMAIN;
        }

        if (exception instanceof TransactionException) {
            return ObservedCustomerProjectionErrorType.TRANSACTION;
        }

        String className =
                exception.getClass().getSimpleName();

        if (className.contains("Audit")) {
            return ObservedCustomerProjectionErrorType.AUDIT;
        }

        return ObservedCustomerProjectionErrorType.UNEXPECTED;
    }

    private static Duration between(
            Instant start,
            Instant end
    ) {
        Duration value = Duration.between(start, end);
        return value.isNegative()
                ? Duration.ZERO
                : value;
    }

    private static Duration lag(
            Instant observedAt,
            Instant completedAt
    ) {
        Duration value = Duration.between(
                observedAt,
                completedAt
        );
        return value.isNegative()
                ? Duration.ZERO
                : value;
    }
}
