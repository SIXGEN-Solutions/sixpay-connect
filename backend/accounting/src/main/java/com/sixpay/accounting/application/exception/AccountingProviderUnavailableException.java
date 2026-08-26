package com.sixpay.accounting.application.exception;

public final class AccountingProviderUnavailableException
        extends RuntimeException {

    public AccountingProviderUnavailableException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
