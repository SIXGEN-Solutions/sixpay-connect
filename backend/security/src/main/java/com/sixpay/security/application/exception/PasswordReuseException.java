package com.sixpay.security.application.exception;

public final class PasswordReuseException extends IllegalArgumentException {
    public PasswordReuseException() {
        super("Password must not reuse the current or a recent password");
    }
}
