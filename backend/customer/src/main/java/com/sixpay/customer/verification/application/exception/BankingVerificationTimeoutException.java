package com.sixpay.customer.verification.application.exception;

/**
 * Connection or read timeout while calling Core Banking.
 */
public final class BankingVerificationTimeoutException
        extends BankingVerificationException {

    public BankingVerificationTimeoutException(
            String message,
            Throwable cause
    ) {
        super(
                message,
                "timeout",
                true,
                cause
        );
    }
}
