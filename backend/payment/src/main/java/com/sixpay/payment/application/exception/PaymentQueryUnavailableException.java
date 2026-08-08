package com.sixpay.payment.application.exception;

public final class PaymentQueryUnavailableException extends RuntimeException {
    public PaymentQueryUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
