package com.sixpay.payment.infrastructure.banking.amplitude.reservation.error;

public final class FundsReservationOutcomeUnknownException
        extends RuntimeException {

    public FundsReservationOutcomeUnknownException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
