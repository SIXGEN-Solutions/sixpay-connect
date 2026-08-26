package com.sixpay.customer.observation.application.exception;

/**
 * Raised when the Observed Customer query projection is temporarily
 * unavailable.
 */
public final class ObservedCustomerQueryUnavailableException
        extends RuntimeException {

    public ObservedCustomerQueryUnavailableException(
            String message
    ) {
        super(requireMessage(message));
    }

    public ObservedCustomerQueryUnavailableException(
            String message,
            Throwable cause
    ) {
        super(
                requireMessage(message),
                cause
        );
    }

    private static String requireMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Observed Customer query projection is unavailable";
        }

        return message.strip();
    }
}
