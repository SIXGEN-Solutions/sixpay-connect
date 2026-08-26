package com.sixpay.customer.verification.application.exception;

/**
 * Base internal exception for failures while obtaining banking verification
 * evidence.
 *
 * <p>These exceptions are application-facing and contain no HTTP DTO,
 * Amplitude payload or sensitive banking value.</p>
 */
public abstract class BankingVerificationException
        extends RuntimeException {

    private final String errorType;
    private final boolean retryable;

    protected BankingVerificationException(
            String message,
            String errorType,
            boolean retryable,
            Throwable cause
    ) {
        super(message, cause);
        this.errorType = errorType;
        this.retryable = retryable;
    }

    public final String errorType() {
        return errorType;
    }

    public final boolean retryable() {
        return retryable;
    }
}
