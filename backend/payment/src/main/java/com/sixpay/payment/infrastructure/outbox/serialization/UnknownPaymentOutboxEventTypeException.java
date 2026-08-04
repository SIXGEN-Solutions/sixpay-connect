package com.sixpay.payment.infrastructure.outbox.serialization;

/**
 * Non-retryable contract error raised for an unknown logical event type.
 */
public final class UnknownPaymentOutboxEventTypeException
        extends RuntimeException {

    public UnknownPaymentOutboxEventTypeException(
            String eventType
    ) {
        super(
                "Unknown Payment outbox event type: "
                        + eventType
        );
    }
}
