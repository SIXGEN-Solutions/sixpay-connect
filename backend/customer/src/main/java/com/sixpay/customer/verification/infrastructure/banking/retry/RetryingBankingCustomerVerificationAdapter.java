package com.sixpay.customer.verification.infrastructure.banking.retry;

import com.sixpay.customer.verification.application.exception.BankingVerificationException;
import com.sixpay.customer.verification.application.exception.BankingVerificationTimeoutException;
import com.sixpay.customer.verification.application.port.output.BankingCustomerVerificationPort;
import com.sixpay.customer.verification.application.port.output.BankingVerificationQuery;
import com.sixpay.customer.verification.application.port.output.BankingVerificationResponse;
import com.sixpay.customer.verification.infrastructure.banking.configuration.BankingVerificationProperties;
import com.sixpay.customer.verification.infrastructure.banking.observability.BankingVerificationObservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Bounded retry and observability decorator for the banking output port.
 *
 * <p>Normal business responses, including FAIL checks, return immediately and
 * are never retried. Only internal exceptions marked retryable are replayed.</p>
 */
public final class RetryingBankingCustomerVerificationAdapter
        implements BankingCustomerVerificationPort {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    RetryingBankingCustomerVerificationAdapter.class
            );

    private final BankingCustomerVerificationPort delegate;
    private final int maxAttempts;
    private final Duration retryBackoff;
    private final RetrySleeper sleeper;
    private final BankingVerificationObservation observation;

    public RetryingBankingCustomerVerificationAdapter(
            BankingCustomerVerificationPort delegate,
            BankingVerificationProperties properties,
            RetrySleeper sleeper,
            BankingVerificationObservation observation
    ) {
        this.delegate = Objects.requireNonNull(delegate);
        Objects.requireNonNull(properties);
        this.maxAttempts = properties.maxAttempts();
        this.retryBackoff = properties.retryBackoff();
        this.sleeper = Objects.requireNonNull(sleeper);
        this.observation = Objects.requireNonNull(observation);
    }

    @Override
    public BankingVerificationResponse verify(
            BankingVerificationQuery query
    ) {
        Objects.requireNonNull(query, "query is required");

        String institution =
                query.financialInstitutionCode().value();
        String verificationId =
                query.verificationId().toString();
        String correlationId =
                query.context().correlationId().value();

        long startedAt = observation.start();
        int attempt = 1;

        while (true) {
            try {
                BankingVerificationResponse response =
                        verifyWithinRemainingBudget(query);

                observation.success(
                        institution,
                        attempt,
                        startedAt
                );

                LOGGER.info(
                        "Banking verification completed: "
                                + "verificationId={}, institution={}, "
                                + "correlationId={}, attempts={}",
                        verificationId,
                        institution,
                        correlationId,
                        attempt
                );

                return response;
            } catch (BankingVerificationException failure) {
                boolean exhausted = attempt >= maxAttempts;
                boolean retry = failure.retryable() && !exhausted;

                if (!retry) {
                    observation.failure(
                            institution,
                            failure.errorType(),
                            attempt,
                            startedAt
                    );

                    LOGGER.warn(
                            "Banking verification failed: "
                                    + "verificationId={}, institution={}, "
                                    + "correlationId={}, errorType={}, "
                                    + "retryable={}, attempts={}",
                            verificationId,
                            institution,
                            correlationId,
                            failure.errorType(),
                            failure.retryable(),
                            attempt
                    );

                    throw failure;
                }

                LOGGER.warn(
                        "Retrying banking verification: "
                                + "verificationId={}, institution={}, "
                                + "correlationId={}, errorType={}, "
                                + "attempt={}, maxAttempts={}",
                        verificationId,
                        institution,
                        correlationId,
                        failure.errorType(),
                        attempt,
                        maxAttempts
                );

                Duration remaining = remaining(query.deadlineAt());
                if (remaining.isZero() || remaining.isNegative()) {
                    throw deadlineExceeded(null);
                }
                Duration boundedBackoff =
                        retryBackoff.compareTo(remaining) < 0
                                ? retryBackoff : remaining;
                sleeper.sleep(boundedBackoff);
                if (!Instant.now().isBefore(query.deadlineAt())) {
                    throw deadlineExceeded(null);
                }
                attempt++;
            }
        }
    }

    private BankingVerificationResponse verifyWithinRemainingBudget(
            BankingVerificationQuery query
    ) {
        Duration remaining = remaining(query.deadlineAt());
        if (remaining.isZero() || remaining.isNegative()) {
            throw deadlineExceeded(null);
        }
        FutureTask<BankingVerificationResponse> task =
                new FutureTask<>(() -> delegate.verify(query));
        Thread worker = Thread.ofVirtual()
                .name("customer-banking-verification")
                .start(task);
        try {
            return task.get(remaining.toNanos(), TimeUnit.NANOSECONDS);
        } catch (TimeoutException timeout) {
            task.cancel(true);
            worker.interrupt();
            throw deadlineExceeded(timeout);
        } catch (InterruptedException interrupted) {
            task.cancel(true);
            worker.interrupt();
            Thread.currentThread().interrupt();
            throw new BankingVerificationTimeoutException(
                    "Customer verification interrupted before deadline completion",
                    interrupted
            );
        } catch (ExecutionException execution) {
            Throwable cause = execution.getCause();
            if (cause instanceof BankingVerificationException bankingFailure) {
                throw bankingFailure;
            }
            if (cause instanceof RuntimeException runtimeFailure) {
                throw runtimeFailure;
            }
            throw new IllegalStateException(
                    "Unexpected Core Banking verification failure", cause
            );
        }
    }

    private static Duration remaining(Instant deadlineAt) {
        if (Instant.MAX.equals(deadlineAt)) {
            return Duration.ofDays(36500);
        }
        return Duration.between(Instant.now(), deadlineAt);
    }

    private static BankingVerificationTimeoutException deadlineExceeded(
            Throwable cause
    ) {
        return new BankingVerificationTimeoutException(
                "Customer verification exceeded the Payment initiation deadline",
                cause
        );
    }
}
