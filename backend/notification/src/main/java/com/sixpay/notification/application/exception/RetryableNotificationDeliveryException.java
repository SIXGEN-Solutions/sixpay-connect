package com.sixpay.notification.application.exception;

public final class RetryableNotificationDeliveryException
        extends RuntimeException {

    private final String errorCode;

    public RetryableNotificationDeliveryException(
            String errorCode,
            String message,
            Throwable cause
    ) {
        super(message, cause);
        this.errorCode = normalize(errorCode);
    }

    public String errorCode() {
        return errorCode;
    }

    private static String normalize(
            String value
    ) {
        return value == null || value.isBlank()
                ? "RETRYABLE_DELIVERY_FAILURE"
                : value.strip();
    }
}
