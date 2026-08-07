package com.sixpay.accounting.application.exception;

public final class AccountingProviderRejectedException
        extends RuntimeException {

    private final int statusCode;

    public AccountingProviderRejectedException(
            String message,
            int statusCode,
            Throwable cause
    ) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int statusCode() {
        return statusCode;
    }
}
