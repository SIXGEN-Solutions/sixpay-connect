package com.sixpay.security.application.port.input;

public final class DirectoryUnavailableException extends RuntimeException {
    public DirectoryUnavailableException(Throwable cause) {
        super("Directory service is unavailable", cause);
    }
}
