package com.sixpay.security.application.exception;

public final class LocalAuthenticationFailedException extends RuntimeException {

    public LocalAuthenticationFailedException() {
        super("Invalid credentials");
    }
}
