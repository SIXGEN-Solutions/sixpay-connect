package com.sixpay.payment.application.port.output;

import java.util.Objects;
import java.util.UUID;

/**
 * Payment-owned representation of a technical Customer Verification failure.
 *
 * <p>The exception contains no Customer, HTTP or Amplitude type. It is always
 * retryable and must never be converted to a business rejection.</p>
 */
public final class CustomerVerificationTechnicalException
        extends RuntimeException {

    private final UUID verificationId;
    private final ErrorType errorType;

    public CustomerVerificationTechnicalException(
            UUID verificationId,
            ErrorType errorType,
            String message,
            Throwable cause
    ) {
        super(
                Objects.requireNonNull(message, "message is required"),
                cause
        );
        this.verificationId = Objects.requireNonNull(
                verificationId,
                "verificationId is required"
        );
        this.errorType = Objects.requireNonNull(
                errorType,
                "errorType is required"
        );
    }

    public UUID verificationId() {
        return verificationId;
    }

    public ErrorType errorType() {
        return errorType;
    }

    public boolean retryable() {
        return true;
    }

    public enum ErrorType {
        TIMEOUT,
        UNAVAILABLE
    }
}
