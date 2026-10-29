package com.sixpay.security.application.port.input;

public final class DirectoryUserNotFoundException extends RuntimeException {
    public DirectoryUserNotFoundException() {
        super("Directory user not found");
    }
}
