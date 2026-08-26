package com.sixpay.payment.infrastructure.outbox.serialization;

/**
 * Non-retryable contract error raised when a known logical event type uses an
 * unsupported schema version.
 */
public final class UnsupportedPaymentOutboxEventVersionException
        extends RuntimeException {

    public UnsupportedPaymentOutboxEventVersionException(
            String eventType,
            int eventVersion
    ) {
        super(
                "Unsupported Payment outbox event version: "
                        + eventType
                        + "@"
                        + eventVersion
        );
    }
}
