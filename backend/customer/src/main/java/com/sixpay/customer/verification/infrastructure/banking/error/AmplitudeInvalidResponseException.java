package com.sixpay.customer.verification.infrastructure.banking.error;

public final class AmplitudeInvalidResponseException
        extends RuntimeException {

    public AmplitudeInvalidResponseException(
            String message
    ) {
        super(message);
    }

    public AmplitudeInvalidResponseException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
