package com.sixpay.accounting.application.exception;

public final class TfjConfirmationConflictException extends RuntimeException {
    public TfjConfirmationConflictException(String message) {
        super(message);
    }
}
