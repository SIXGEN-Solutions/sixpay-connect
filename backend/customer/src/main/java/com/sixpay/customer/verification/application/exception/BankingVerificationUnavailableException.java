package com.sixpay.customer.verification.application.exception;

/**
 * Temporary Core Banking unavailability, including connection failures and
 * retryable upstream HTTP statuses.
 */
public final class BankingVerificationUnavailableException
        extends BankingVerificationException {

    public BankingVerificationUnavailableException(
            String message,
            Throwable cause
    ) {
        super(
                message,
                "unavailable",
                true,
                cause
        );
    }
}
