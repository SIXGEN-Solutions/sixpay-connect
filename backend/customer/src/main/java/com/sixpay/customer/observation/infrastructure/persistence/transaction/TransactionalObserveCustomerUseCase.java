package com.sixpay.customer.observation.infrastructure.persistence.transaction;

import com.sixpay.customer.observation.application.port.input
        .ObserveCustomerCommand;
import com.sixpay.customer.observation.application.port.input
        .ObserveCustomerResult;
import com.sixpay.customer.observation.application.port.input
        .ObserveCustomerUseCase;
import com.sixpay.customer.observation.infrastructure.observability
        .ObservedCustomerProjectionMetrics;
import com.sixpay.customer.observation.infrastructure.observability
        .ObservedCustomerProjectionObservation;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;

/**
 * Executes the complete projection mutation in one transaction.
 */
public final class TransactionalObserveCustomerUseCase
        implements ObserveCustomerUseCase {

    private final ObserveCustomerUseCase delegate;
    private final TransactionTemplate transactionTemplate;
    private final int maxAttempts;
    private final ObservedCustomerProjectionMetrics metrics;

    /**
     * Compatibility constructor used by existing tests and isolated contexts.
     */
    public TransactionalObserveCustomerUseCase(
            ObserveCustomerUseCase delegate,
            PlatformTransactionManager transactionManager,
            int maxAttempts
    ) {
        this(
                delegate,
                transactionManager,
                maxAttempts,
                null
        );
    }

    public TransactionalObserveCustomerUseCase(
            ObserveCustomerUseCase delegate,
            PlatformTransactionManager transactionManager,
            int maxAttempts,
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

        if (maxAttempts < 1 || maxAttempts > 10) {
            throw new IllegalArgumentException(
                    "maxAttempts must be between 1 and 10"
            );
        }

        this.maxAttempts = maxAttempts;
        this.metrics = metrics;
    }

    @Override
    public ObserveCustomerResult observe(
            ObserveCustomerCommand command
    ) {
        Objects.requireNonNull(command, "command is required");

        RuntimeException lastFailure = null;

        for (int attempt = 1;
             attempt <= maxAttempts;
             attempt++) {

            markAttempt(attempt);

            try {
                return transactionTemplate.execute(
                        status -> delegate.observe(command)
                );
            } catch (ObjectOptimisticLockingFailureException
                     | DataIntegrityViolationException exception) {
                lastFailure = exception;

                if (attempt == maxAttempts) {
                    throw exception;
                }

                recordRetry(
                        command,
                        attempt,
                        exception
                );
            }
        }

        throw new IllegalStateException(
                "Observed Customer transaction retry exhausted",
                lastFailure
        );
    }

    private void markAttempt(int attempt) {
        if (metrics != null) {
            metrics.attempt(attempt);
        }
    }

    private void recordRetry(
            ObserveCustomerCommand command,
            int failedAttempt,
            RuntimeException exception
    ) {
        if (metrics != null) {
            metrics.retry(
                    command,
                    failedAttempt,
                    ObservedCustomerProjectionObservation
                            .classify(exception)
            );
        }
    }
}
