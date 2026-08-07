package com.sixpay.accounting.domain.exception;

public final class AccountingBatchPersistenceConflictException
        extends RuntimeException {

    public AccountingBatchPersistenceConflictException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
