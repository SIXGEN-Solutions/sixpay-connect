package com.sixpay.customer.verification.application.exception;

/**
 * Non-retryable malformed, incomplete or contract-incompatible response.
 */
public final class BankingVerificationInvalidResponseException
        extends BankingVerificationException {

    public BankingVerificationInvalidResponseException(
            String message,
            Throwable cause
    ) {
        super(
                message,
                "invalid_response",
                false,
                cause
        );
    }
}
