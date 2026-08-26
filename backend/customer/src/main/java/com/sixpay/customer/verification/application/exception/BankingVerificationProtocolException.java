package com.sixpay.customer.verification.application.exception;

/**
 * Non-retryable HTTP or protocol failure.
 */
public final class BankingVerificationProtocolException
        extends BankingVerificationException {

    public BankingVerificationProtocolException(
            String message,
            Throwable cause
    ) {
        super(
                message,
                "protocol",
                false,
                cause
        );
    }
}
