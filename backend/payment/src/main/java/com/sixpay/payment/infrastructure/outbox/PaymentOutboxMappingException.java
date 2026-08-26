package com.sixpay.payment.infrastructure.outbox;

public final class PaymentOutboxMappingException extends RuntimeException {

    public PaymentOutboxMappingException(String message, Throwable cause) {
        super(message, cause);
    }
}
