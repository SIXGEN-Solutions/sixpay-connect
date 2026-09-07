package com.sixpay.payment.infrastructure.banking.amplitude.posting.error;

public final class AmplitudePaymentEventOutcomeUnknownException
        extends RuntimeException {

    public AmplitudePaymentEventOutcomeUnknownException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
