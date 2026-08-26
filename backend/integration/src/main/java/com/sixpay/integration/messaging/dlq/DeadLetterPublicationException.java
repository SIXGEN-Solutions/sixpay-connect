package com.sixpay.integration.messaging.dlq;
public final class DeadLetterPublicationException extends RuntimeException {
    public DeadLetterPublicationException(Throwable cause) {
        super("Unable to publish dead-letter record", cause);
    }
}
