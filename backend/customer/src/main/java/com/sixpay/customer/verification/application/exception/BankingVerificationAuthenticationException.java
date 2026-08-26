package com.sixpay.customer.verification.application.exception;

/**
 * Non-retryable Core Banking authentication or authorization failure.
 */
public final class BankingVerificationAuthenticationException
        extends BankingVerificationException {

    public BankingVerificationAuthenticationException(
            String message,
            Throwable cause
    ) {
        super(
                message,
                "authentication",
                false,
                cause
        );
    }
}
