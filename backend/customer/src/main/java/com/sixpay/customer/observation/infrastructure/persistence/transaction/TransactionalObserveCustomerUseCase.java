package com.sixpay.customer.observation.infrastructure.persistence.transaction;

import com.sixpay.customer.observation.application.port.input
        .ObserveCustomerCommand;
import com.sixpay.customer.observation.application.port.input
        .ObserveCustomerResult;
import com.sixpay.customer.observation.application.port.input
        .ObserveCustomerUseCase;
import com.sixpay.customer.observation.infrastructure.observability
        .ObservedCustomerProjectionErrorType;
import com.sixpay.customer.observation.infrastructure.observability
        .ObservedCustomerProjectionMetrics;
import com.sixpay.customer.observation.infrastructure.resilience
        .ObservedCustomerProjectionFailureClassifier;
import com.sixpay.customer.observation.infrastructure.resilience
        .ObservedCustomerProjectionFailureType;
import com.sixpay.customer.observation.infrastructure.resilience
        .ObservedCustomerProjectionRetryExhaustedException;
import com.sixpay.customer.observation.infrastructure.resilience
        .ObservedCustomerProjectionRetryPolicy;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.Objects;

/**
 * Executes the complete projection mutation input a bounded retry loop.
 *
 * <p>Each attempt receives a fresh transaction. Idempotence races are retried
 * by invoking the application service again, which reloads the winning
 * projection and can return REPLAYED or continue from the winning state.</p>
 */
public final class TransactionalObserveCustomerUseCase
        implements ObserveCustomerUseCase {

    private final ObserveCustomerUseCase delegate;
    private final TransactionTemplate transactionTemplate;
    private final ObservedCustomerProjectionFailureClassifier classifier;
    private final ObservedCustomerProjectionRetryPolicy retryPolicy;
    private final ObservedCustomerProjectionMetrics metrics;

    /**
     * Compatibility constructor for existing isolated tests.
     */
    public TransactionalObserveCustomerUseCase(
            ObserveCustomerUseCase delegate,
            PlatformTransactionManager transactionManager,
            int maxAttempts
    ) {
        this(
                delegate,
                transactionManager,
                new ObservedCustomerProjectionFailureClassifier(),
                compatibilityPolicy(maxAttempts),
                null
        );
    }

    /**
     * Compatibility constructor introduced by lot 4.8.4.
     */
    public TransactionalObserveCustomerUseCase(
            ObserveCustomerUseCase delegate,
            PlatformTransactionManager transactionManager,
            int maxAttempts,
            ObservedCustomerProjectionMetrics metrics
    ) {
        this(
                delegate,
                transactionManager,
                new ObservedCustomerProjectionFailureClassifier(),
                compatibilityPolicy(maxAttempts),
                metrics
        );
    }

    public TransactionalObserveCustomerUseCase(
            ObserveCustomerUseCase delegate,
            PlatformTransactionManager transactionManager,
            ObservedCustomerProjectionFailureClassifier classifier,
            ObservedCustomerProjectionRetryPolicy retryPolicy,
            ObservedCustomerProjectionMetrics metrics
    ) {
        this.delegate = Objects.requireNonNull(
                delegate,
                "delegate is required"
        );
        this.transactionTemplate = new TransactionTemplate(
                Objects.requireNonNull(
                        transactionManager,
                        "transactionManager is required"
                )
        );
        this.classifier = Objects.requireNonNull(
                classifier,
                "classifier is required"
        );
        this.retryPolicy = Objects.requireNonNull(
                retryPolicy,
                "retryPolicy is required"
        );
        this.metrics = metrics;
    }

    @Override
    public ObserveCustomerResult observe(
            ObserveCustomerCommand command
    ) {
        Objects.requireNonNull(command, "command is required");

        for (int attempt = 1;
             attempt <= retryPolicy.maxAttempts();
             attempt++) {

            markAttempt(attempt);

            try {
                return transactionTemplate.execute(
                        status -> delegate.observe(command)
                );
            } catch (RuntimeException failure) {
                ObservedCustomerProjectionFailureType failureType =
                        classifier.classify(failure);

                if (!failureType.retryable()) {
                    throw failure;
                }

                if (!retryPolicy.shouldRetry(
                        attempt,
                        failureType
                )) {
                    throw new
                            ObservedCustomerProjectionRetryExhaustedException(
                                    attempt,
                                    failureType,
                                    failure
                            );
                }

                recordRetry(
                        command,
                        attempt,
                        failureType
                );

                retryPolicy.beforeRetry(attempt);
            }
        }

        throw new IllegalStateException(
                "Observed Customer retry loop ended unexpectedly"
        );
    }

    private void markAttempt(int attempt) {
        if (metrics != null) {
            metrics.attempt(attempt);
        }
    }

    private void recordRetry(
            ObserveCustomerCommand command,
            int attempt,
            ObservedCustomerProjectionFailureType failureType
    ) {
        if (metrics != null) {
            metrics.retry(
                    command,
                    attempt,
                    metricErrorType(failureType)
            );
        }
    }

    private static ObservedCustomerProjectionErrorType
    metricErrorType(
            ObservedCustomerProjectionFailureType failureType
    ) {
        return switch (failureType) {
            case OPTIMISTIC_LOCK ->
                    ObservedCustomerProjectionErrorType
                            .OPTIMISTIC_LOCK;
            case IDEMPOTENCE_RACE ->
                    ObservedCustomerProjectionErrorType
                            .DATA_INTEGRITY;
            case DEADLOCK,
                 SERIALIZATION_FAILURE,
                 TRANSIENT_TRANSACTION,
                 TEMPORARY_CONNECTION ->
                    ObservedCustomerProjectionErrorType
                            .TRANSACTION;
            default ->
                    ObservedCustomerProjectionErrorType
                            .UNEXPECTED;
        };
    }

    private static ObservedCustomerProjectionRetryPolicy
    compatibilityPolicy(int maxAttempts) {
        return new ObservedCustomerProjectionRetryPolicy(
                maxAttempts,
                Duration.ofNanos(1),
                Duration.ofNanos(1),
                1.0,
                0.0,
                ignored -> {
                    // Compatibility mode intentionally has no wait.
                },
                () -> 0.5
        );
    }
}
