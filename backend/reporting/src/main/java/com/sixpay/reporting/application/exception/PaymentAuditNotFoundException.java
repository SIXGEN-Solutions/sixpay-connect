package com.sixpay.reporting.application.exception;

public final class PaymentAuditNotFoundException extends RuntimeException {
    public PaymentAuditNotFoundException(String message) {
        super(message);
    }
}
