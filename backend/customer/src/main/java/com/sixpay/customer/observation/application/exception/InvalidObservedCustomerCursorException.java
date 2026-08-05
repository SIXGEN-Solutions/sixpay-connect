package com.sixpay.customer.observation.application.exception;

/**
 * Client-visible validation error for an invalid, altered or incompatible
 * Observed Customer cursor.
 */
public final class InvalidObservedCustomerCursorException
        extends IllegalArgumentException {

    public InvalidObservedCustomerCursorException(
            String message
    ) {
        super(
                message == null || message.isBlank()
                        ? "Observed Customer cursor is invalid"
                        : message.strip()
        );
    }

    public InvalidObservedCustomerCursorException(
            String message,
            Throwable cause
    ) {
        super(
                message == null || message.isBlank()
                        ? "Observed Customer cursor is invalid"
                        : message.strip(),
                cause
        );
    }
}
