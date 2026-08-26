package com.sixpay.customer.observation.infrastructure.resilience;

import com.sixpay.customer.observation.domain.exception
        .ObservedCustomerDomainException;
import jakarta.persistence.OptimisticLockException;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.TransactionException;

import java.sql.SQLException;
import java.util.Locale;
import java.util.Objects;

/**
 * Classifies projection failures into bounded technical categories.
 *
 * <p>Free-form exception messages are not used as operational
 * classifications, except for known PostgreSQL constraint identifiers
 * required to distinguish expected idempotence races from unrelated
 * integrity violations.</p>
 */
public final class ObservedCustomerProjectionFailureClassifier {

    private static final String SQLSTATE_SERIALIZATION =
            "40001";

    private static final String SQLSTATE_DEADLOCK =
            "40P01";

    private static final String SQLSTATE_UNIQUE_VIOLATION =
            "23505";

    public ObservedCustomerProjectionFailureType classify(
            Throwable failure
    ) {
        Objects.requireNonNull(
                failure,
                "failure is required"
        );

        if (hasCause(
                failure,
                ObjectOptimisticLockingFailureException.class
        ) || hasCause(
                failure,
                OptimisticLockException.class
        )) {
            return ObservedCustomerProjectionFailureType
                    .OPTIMISTIC_LOCK;
        }

        SQLException sqlException =
                findCause(
                        failure,
                        SQLException.class
                );

        if (sqlException != null) {
            ObservedCustomerProjectionFailureType sqlFailure =
                    classifySqlException(
                            sqlException
                    );

            if (sqlFailure != null) {
                return sqlFailure;
            }
        }

        if (hasCause(
                failure,
                CannotAcquireLockException.class
        )) {
            return ObservedCustomerProjectionFailureType
                    .DEADLOCK;
        }

        if (hasCause(
                failure,
                CannotGetJdbcConnectionException.class
        ) || hasCause(
                failure,
                DataAccessResourceFailureException.class
        )) {
            return ObservedCustomerProjectionFailureType
                    .TEMPORARY_CONNECTION;
        }

        if (hasCause(
                failure,
                DataIntegrityViolationException.class
        )) {
            if (isKnownIdempotenceConstraint(
                    rootMessage(failure)
            )) {
                return ObservedCustomerProjectionFailureType
                        .IDEMPOTENCE_RACE;
            }

            return ObservedCustomerProjectionFailureType
                    .NON_RETRYABLE;
        }

        if (hasCause(
                failure,
                TransientDataAccessException.class
        ) || hasCause(
                failure,
                TransactionException.class
        ) || hasCause(
                failure,
                UncategorizedSQLException.class
        )) {
            return ObservedCustomerProjectionFailureType
                    .TRANSIENT_TRANSACTION;
        }

        if (hasCause(
                failure,
                ObservedCustomerDomainException.class
        ) || hasCause(
                failure,
                IllegalArgumentException.class
        ) || hasCause(
                failure,
                NullPointerException.class
        )) {
            return classifyPermanentInputFailure(
                    failure
            );
        }

        if (isCryptographyFailure(
                failure
        )) {
            return ObservedCustomerProjectionFailureType
                    .PERMANENT_CRYPTOGRAPHY;
        }

        return ObservedCustomerProjectionFailureType
                .NON_RETRYABLE;
    }

    private static ObservedCustomerProjectionFailureType
    classifySqlException(
            SQLException exception
    ) {
        String sqlState =
                exception.getSQLState();

        if (SQLSTATE_DEADLOCK.equals(
                sqlState
        )) {
            return ObservedCustomerProjectionFailureType
                    .DEADLOCK;
        }

        if (SQLSTATE_SERIALIZATION.equals(
                sqlState
        )) {
            return ObservedCustomerProjectionFailureType
                    .SERIALIZATION_FAILURE;
        }

        if (sqlState != null && sqlState.startsWith("08")) {
            return ObservedCustomerProjectionFailureType
                    .TEMPORARY_CONNECTION;
        }

        if (SQLSTATE_UNIQUE_VIOLATION.equals(
                sqlState
        )) {
            if (isKnownIdempotenceConstraint(
                    exception.getMessage()
            )) {
                return ObservedCustomerProjectionFailureType
                        .IDEMPOTENCE_RACE;
            }

            return ObservedCustomerProjectionFailureType
                    .NON_RETRYABLE;
        }

        return null;
    }

    private static ObservedCustomerProjectionFailureType
    classifyPermanentInputFailure(
            Throwable failure
    ) {
        String message =
                rootMessage(
                        failure
                ).toLowerCase(
                        Locale.ROOT
                );

        if (message.contains(
                "identity"
        ) || message.contains(
                "niu"
        ) || message.contains(
                "contradict"
        )) {
            return ObservedCustomerProjectionFailureType
                    .CONTRADICTORY_IDENTITY;
        }

        if (message.contains(
                "status"
        )) {
            return ObservedCustomerProjectionFailureType
                    .UNKNOWN_STATUS;
        }

        if (message.contains(
                "required"
        ) || message.contains(
                "must not be null"
        ) || message.contains(
                "missing"
        )) {
            return ObservedCustomerProjectionFailureType
                    .MISSING_REQUIRED_DATA;
        }

        if (message.contains(
                "version"
        ) || message.contains(
                "contract"
        ) || message.contains(
                "schema"
        )) {
            return ObservedCustomerProjectionFailureType
                    .INCOMPATIBLE_CONTRACT;
        }

        return ObservedCustomerProjectionFailureType
                .INVALID_PAYLOAD;
    }

    private static boolean isKnownIdempotenceConstraint(
            String message
    ) {
        if (message == null) {
            return false;
        }

        String normalized =
                message.toLowerCase(
                        Locale.ROOT
                );

        return normalized.contains(
                "customer_observation_processed_event_pkey"
        ) || normalized.contains(
                "uk_customer_observed_customer_niu_hash"
        ) || normalized.contains(
                "customer_observed_customer_pkey"
        ) || normalized.contains(
                "customer_observed_payment_pkey"
        );
    }

    private static boolean isCryptographyFailure(
            Throwable failure
    ) {
        for (
                Throwable current = failure;
                current != null;
                current = current.getCause()
        ) {
            String className =
                    current.getClass()
                            .getName();

            if (className.startsWith(
                    "java.security."
            ) || className.startsWith(
                    "javax.crypto."
            ) || className.startsWith(
                    "jakarta.crypto."
            ) || className.contains(
                    "AEADBadTag"
            ) || className.contains(
                    "InvalidKey"
            )) {
                return true;
            }
        }

        return false;
    }

    private static String rootMessage(
            Throwable failure
    ) {
        Throwable current =
                failure;

        while (
                current.getCause() != null
                        && current.getCause() != current
        ) {
            current =
                    current.getCause();
        }

        return current.getMessage() == null
                ? ""
                : current.getMessage();
    }

    private static <T extends Throwable> boolean hasCause(
            Throwable failure,
            Class<T> type
    ) {
        return findCause(
                failure,
                type
        ) != null;
    }

    private static <T extends Throwable> T findCause(
            Throwable failure,
            Class<T> type
    ) {
        for (
                Throwable current = failure;
                current != null;
                current = current.getCause()
        ) {
            if (type.isInstance(
                    current
            )) {
                return type.cast(
                        current
                );
            }
        }

        return null;
    }
}