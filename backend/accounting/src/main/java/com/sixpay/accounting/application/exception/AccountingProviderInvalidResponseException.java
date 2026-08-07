package com.sixpay.accounting.application.exception;

public final class AccountingProviderInvalidResponseException
        extends RuntimeException {

    public AccountingProviderInvalidResponseException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
