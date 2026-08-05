package com.sixpay.customer.observation.infrastructure.resilience;

import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ObservedCustomerProjectionFailureClassifierTest {

    private final ObservedCustomerProjectionFailureClassifier classifier =
            new ObservedCustomerProjectionFailureClassifier();

    @Test
    void optimisticLockIsRetryable() {
        assertEquals(
                ObservedCustomerProjectionFailureType
                        .OPTIMISTIC_LOCK,
                classifier.classify(
                        new OptimisticLockException("conflict")
                )
        );
    }

    @Test
    void serializationAndDeadlockAreExplicitlyClassified() {
        assertEquals(
                ObservedCustomerProjectionFailureType
                        .SERIALIZATION_FAILURE,
                classifier.classify(
                        sqlFailure("40001", "serialization")
                )
        );

        assertEquals(
                ObservedCustomerProjectionFailureType.DEADLOCK,
                classifier.classify(
                        sqlFailure("40P01", "deadlock")
                )
        );
    }

    @Test
    void temporaryConnectionIsRetryable() {
        assertEquals(
                ObservedCustomerProjectionFailureType
                        .TEMPORARY_CONNECTION,
                classifier.classify(
                        sqlFailure("08006", "connection lost")
                )
        );
    }

    @Test
    void knownUniqueConstraintIsAnIdempotenceRace() {
        DataIntegrityViolationException failure =
                new DataIntegrityViolationException(
                        "duplicate",
                        new SQLException(
                                "duplicate key violates constraint "
                                        + "\"customer_observation_"
                                        + "processed_event_pkey\"",
                                "23505"
                        )
                );

        assertEquals(
                ObservedCustomerProjectionFailureType
                        .IDEMPOTENCE_RACE,
                classifier.classify(failure)
        );
    }

    @Test
    void unknownUniqueConstraintIsNotRetried() {
        DataIntegrityViolationException failure =
                new DataIntegrityViolationException(
                        "duplicate",
                        new SQLException(
                                "duplicate key violates constraint "
                                        + "\"unrelated_unique_key\"",
                                "23505"
                        )
                );

        assertEquals(
                ObservedCustomerProjectionFailureType
                        .NON_RETRYABLE,
                classifier.classify(failure)
        );
    }

    @Test
    void missingRequiredDataIsPermanent() {
        assertEquals(
                ObservedCustomerProjectionFailureType
                        .MISSING_REQUIRED_DATA,
                classifier.classify(
                        new IllegalArgumentException(
                                "paymentId is required"
                        )
                )
        );
    }

    private static RuntimeException sqlFailure(
            String sqlState,
            String message
    ) {
        return new RuntimeException(
                new SQLException(message, sqlState)
        );
    }
}
