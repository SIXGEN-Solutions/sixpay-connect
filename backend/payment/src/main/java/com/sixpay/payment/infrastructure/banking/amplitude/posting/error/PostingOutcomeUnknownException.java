package com.sixpay.payment.infrastructure.banking.amplitude.posting.error;

public final class PostingOutcomeUnknownException
        extends RuntimeException {

    public PostingOutcomeUnknownException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
