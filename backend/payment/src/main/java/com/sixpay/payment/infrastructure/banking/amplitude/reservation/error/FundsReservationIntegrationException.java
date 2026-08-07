package com.sixpay.payment.infrastructure.banking.amplitude.reservation.error;

public final class FundsReservationIntegrationException
        extends RuntimeException {

    private final int httpStatus;
    private final boolean retryable;

    public FundsReservationIntegrationException(
            String message,
            int httpStatus,
            boolean retryable,
            Throwable cause
    ) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.retryable = retryable;
    }

    public int httpStatus() {
        return httpStatus;
    }

    public boolean retryable() {
        return retryable;
    }
}
