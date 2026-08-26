package com.sixpay.accounting.application.exception;

public final class AccountingProviderAuthenticationException
        extends RuntimeException {

    public AccountingProviderAuthenticationException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
