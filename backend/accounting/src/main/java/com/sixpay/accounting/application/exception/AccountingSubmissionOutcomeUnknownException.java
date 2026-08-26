package com.sixpay.accounting.application.exception;

public final class AccountingSubmissionOutcomeUnknownException
        extends RuntimeException {

    public AccountingSubmissionOutcomeUnknownException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
