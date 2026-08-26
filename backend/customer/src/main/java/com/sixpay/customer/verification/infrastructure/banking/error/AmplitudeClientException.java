package com.sixpay.customer.verification.infrastructure.banking.error;

public final class AmplitudeClientException extends RuntimeException {

    private final int httpStatus;
    private final AmplitudeErrorResponse error;

    public AmplitudeClientException(
            int httpStatus,
            AmplitudeErrorResponse error,
            Throwable cause
    ) {
        super(
                "Core Banking verification failed with HTTP "
                        + httpStatus
                        + " and code "
                        + (error == null ? "UNKNOWN" : error.code()),
                cause
        );
        this.httpStatus = httpStatus;
        this.error = error;
    }

    public int httpStatus() {
        return httpStatus;
    }

    public AmplitudeErrorResponse error() {
        return error;
    }
}
