package com.sixpay.customer.observation.infrastructure.persistence.transaction;

import com.sixpay.customer.observation.application.port.input.ObserveCustomerCommand;
import com.sixpay.customer.observation.application.port.input.ObserveCustomerResult;
import com.sixpay.customer.observation.application.port.input.ObserveCustomerUseCase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;

/**
 * Executes the full projection mutation in one transaction and retries bounded
 * optimistic-lock or idempotence races by reloading the projection.
 */
public final class TransactionalObserveCustomerUseCase
        implements ObserveCustomerUseCase {

    private final ObserveCustomerUseCase delegate;
    private final TransactionTemplate transactionTemplate;
    private final int maxAttempts;

    public TransactionalObserveCustomerUseCase(
            ObserveCustomerUseCase delegate,
            PlatformTransactionManager transactionManager,
            int maxAttempts
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
    }

    @Override
    public ObserveCustomerResult observe(
            ObserveCustomerCommand command
    ) {
        RuntimeException lastFailure = null;

        for (int attempt = 1;
             attempt <= maxAttempts;
             attempt++) {
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
            }
        }

        throw new IllegalStateException(
                "Observed Customer transaction retry exhausted",
                lastFailure
        );
    }
}
