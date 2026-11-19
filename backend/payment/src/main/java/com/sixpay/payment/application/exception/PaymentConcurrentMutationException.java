package com.sixpay.payment.application.exception;

/**
 * A Payment aggregate lost an optimistic write race. Callers may acknowledge
 * the operation only after independently proving the same effect is durable.
 * Blind retries are forbidden for operations involving external banking I/O.
 */
public final class PaymentConcurrentMutationException extends RuntimeException {
    public PaymentConcurrentMutationException(String message) {
        super(message);
    }

    public PaymentConcurrentMutationException(String message, Throwable cause) {
        super(message, cause);
    }
}
