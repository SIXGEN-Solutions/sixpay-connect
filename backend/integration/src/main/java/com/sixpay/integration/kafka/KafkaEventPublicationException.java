package com.sixpay.integration.kafka;

public final class KafkaEventPublicationException
        extends RuntimeException {

    public KafkaEventPublicationException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
