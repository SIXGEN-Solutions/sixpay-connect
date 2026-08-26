package com.sixpay.customer.observation.infrastructure.persistence.transaction;

import com.sixpay.customer.observation.application.port.input
        .ObserveCustomerCommand;
import com.sixpay.customer.observation.application.port.input
        .ObserveCustomerResult;
import com.sixpay.customer.observation.application.port.input
        .ObserveCustomerUseCase;
import com.sixpay.customer.observation.domain.model
        .ObservedCustomerId;
import com.sixpay.customer.observation.domain.model
        .ObservedPaymentStatus;
import com.sixpay.customer.observation.infrastructure.resilience
        .ObservedCustomerProjectionFailureClassifier;
import com.sixpay.customer.observation.infrastructure.resilience
        .ObservedCustomerProjectionFailureType;
import com.sixpay.customer.observation.infrastructure.resilience
        .ObservedCustomerProjectionRetryExhaustedException;
import com.sixpay.customer.observation.infrastructure.resilience
        .ObservedCustomerProjectionRetryPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionalObserveCustomerUseCaseResilienceTest {

    private static final Instant NOW =
            Instant.parse("2026-08-05T18:00:00Z");

    @Test
    void idempotenceRaceReloadsAndCanReturnReplayed() {
        AtomicInteger calls = new AtomicInteger();

        ObserveCustomerUseCase delegate = ignored -> {
            if (calls.incrementAndGet() == 1) {
                throw duplicateSourceEvent();
            }

            return replayed();
        };

        TransactionalObserveCustomerUseCase useCase =
                useCase(delegate, 3);

        ObserveCustomerResult result =
                useCase.observe(command());

        assertEquals(
                ObserveCustomerResult.Disposition.REPLAYED,
                result.disposition()
        );
        assertEquals(2, calls.get());
    }

    @Test
    void permanentFailureIsNotRetried() {
        AtomicInteger calls = new AtomicInteger();
        IllegalArgumentException failure =
                new IllegalArgumentException(
                        "paymentId is required"
                );

        ObserveCustomerUseCase delegate = ignored -> {
            calls.incrementAndGet();
            throw failure;
        };

        assertSame(
                failure,
                assertThrows(
                        IllegalArgumentException.class,
                        () -> useCase(delegate, 3)
                                .observe(command())
                )
        );
        assertEquals(1, calls.get());
    }

    @Test
    void retryableFailureBecomesExplicitExhaustedException() {
        ObserveCustomerUseCase delegate =
                ignored -> {
                    throw duplicateSourceEvent();
                };

        ObservedCustomerProjectionRetryExhaustedException exhausted =
                assertThrows(
                        ObservedCustomerProjectionRetryExhaustedException.class,
                        () -> useCase(delegate, 2)
                                .observe(command())
                );

        assertEquals(2, exhausted.attempts());
        assertEquals(
                ObservedCustomerProjectionFailureType
                        .IDEMPOTENCE_RACE,
                exhausted.failureType()
        );
    }

    private static TransactionalObserveCustomerUseCase useCase(
            ObserveCustomerUseCase delegate,
            int attempts
    ) {
        return new TransactionalObserveCustomerUseCase(
                delegate,
                transactionManager(),
                new ObservedCustomerProjectionFailureClassifier(),
                new ObservedCustomerProjectionRetryPolicy(
                        attempts,
                        Duration.ofNanos(1),
                        Duration.ofNanos(1),
                        1.0,
                        0.0,
                        ignored -> {
                        },
                        () -> 0.5
                ),
                null
        );
    }

    private static RuntimeException duplicateSourceEvent() {
        return new DataIntegrityViolationException(
                "duplicate",
                new SQLException(
                        "duplicate key violates constraint "
                                + "\"customer_observation_"
                                + "processed_event_pkey\"",
                        "23505"
                )
        );
    }

    private static ObserveCustomerResult replayed() {
        return new ObserveCustomerResult(
                ObservedCustomerId.of(
                        UUID.fromString(
                                "44444444-4444-4444-8444-444444444444"
                        )
                ),
                command().sourceEventId(),
                command().paymentId(),
                ObserveCustomerResult.Disposition.REPLAYED,
                1,
                NOW
        );
    }

    private static ObserveCustomerCommand command() {
        return new ObserveCustomerCommand(
                UUID.fromString(
                        "11111111-1111-4111-8111-111111111111"
                ),
                UUID.fromString(
                        "22222222-2222-4222-8222-222222222222"
                ),
                "PAY-001",
                "M0123456",
                "Société ABC",
                "***-***-1234",
                "a***@example.com",
                "BANK",
                "v1:" + "a".repeat(64),
                "•••• 1234",
                new BigDecimal("100.00"),
                "XAF",
                ObservedPaymentStatus.RECEIVED,
                null,
                NOW.minusSeconds(10),
                NOW,
                NOW,
                "55555555-5555-4555-8555-555555555555"
        );
    }

    private static PlatformTransactionManager
    transactionManager() {
        return new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(
                    TransactionDefinition definition
            ) {
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) {
            }

            @Override
            public void rollback(TransactionStatus status) {
            }
        };
    }
}
