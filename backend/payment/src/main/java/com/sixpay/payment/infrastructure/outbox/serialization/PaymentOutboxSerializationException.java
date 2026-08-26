package com.sixpay.payment.infrastructure.outbox.serialization;

/**
 * Signals that a supported Payment outbox contract could not be encoded or
 * decoded.
 */
public final class PaymentOutboxSerializationException
        extends RuntimeException {

    public PaymentOutboxSerializationException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }

    public PaymentOutboxSerializationException(
            String message
    ) {
        super(message);
    }
}
