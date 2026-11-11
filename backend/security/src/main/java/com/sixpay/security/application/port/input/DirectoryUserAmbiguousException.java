package com.sixpay.security.application.port.input;

public final class DirectoryUserAmbiguousException extends RuntimeException {
    public DirectoryUserAmbiguousException() {
        super("Directory user lookup is ambiguous");
    }
}
