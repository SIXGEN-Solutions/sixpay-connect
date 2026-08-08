package com.sixpay.reporting.application.exception;

public final class PaymentAuditQueryUnavailableException extends RuntimeException {
    public PaymentAuditQueryUnavailableException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
