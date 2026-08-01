package com.sixpay.payment.infrastructure.persistence;

/**
 * Stable infrastructure exception raised when Payment persistence cannot
 * preserve aggregate consistency.
 */
public final class PaymentPersistenceException extends RuntimeException {

    public PaymentPersistenceException(String message) {
        super(message);
    }

    public PaymentPersistenceException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
