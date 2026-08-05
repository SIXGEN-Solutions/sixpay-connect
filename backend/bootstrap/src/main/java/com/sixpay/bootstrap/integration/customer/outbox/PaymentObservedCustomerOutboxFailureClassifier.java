package com.sixpay.bootstrap.integration.customer.outbox;

import com.sixpay.customer.observation.domain.exception.ObservedCustomerDomainException;
import com.sixpay.payment.infrastructure.outbox.serialization.PaymentOutboxSerializationException;
import com.sixpay.payment.infrastructure.outbox.serialization.UnknownPaymentOutboxEventTypeException;
import com.sixpay.payment.infrastructure.outbox.serialization.UnsupportedPaymentOutboxEventVersionException;
import jakarta.persistence.OptimisticLockException;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessException;

import java.util.Objects;

/**
 * Classifies delivery failures using bounded error types only.
 */
public final class PaymentObservedCustomerOutboxFailureClassifier {

    public Classification classify(Throwable failure) {
        Objects.requireNonNull(failure, "failure is required");

        if (contains(failure, UnknownPaymentOutboxEventTypeException.class)) {
            return Classification.nonRetryable("unknown_event_type");
        }

        if (contains(
                failure,
                UnsupportedPaymentOutboxEventVersionException.class
        )) {
            return Classification.nonRetryable("unsupported_event_version");
        }

        if (contains(failure, PaymentOutboxSerializationException.class)) {
            return Classification.nonRetryable("invalid_event_payload");
        }

        if (contains(failure, ObservedCustomerDomainException.class)) {
            return Classification.nonRetryable("projection_domain_conflict");
        }

        if (contains(failure, IllegalArgumentException.class)) {
            return Classification.nonRetryable("invalid_contract");
        }

        if (contains(failure, OptimisticLockException.class)
                || contains(failure, CannotAcquireLockException.class)
                || contains(failure, QueryTimeoutException.class)
                || contains(failure, DataAccessResourceFailureException.class)
                || contains(failure, TransientDataAccessException.class)) {
            return Classification.retryable("temporary_persistence_failure");
        }

        return Classification.retryable("temporary_infrastructure_failure");
    }

    private static boolean contains(
            Throwable failure,
            Class<? extends Throwable> expectedType
    ) {
        Throwable current = failure;
        while (current != null) {
            if (expectedType.isInstance(current)) {
                return true;
            }
            if (current.getCause() == current) {
                return false;
            }
            current = current.getCause();
        }
        return false;
    }

    public record Classification(
            boolean retryable,
            String errorType
    ) {
        public Classification {
            if (errorType == null || errorType.isBlank()) {
                throw new IllegalArgumentException("errorType must not be blank");
            }
            errorType = errorType.strip();
        }

        public static Classification retryable(String errorType) {
            return new Classification(true, errorType);
        }

        public static Classification nonRetryable(String errorType) {
            return new Classification(false, errorType);
        }
    }
}
