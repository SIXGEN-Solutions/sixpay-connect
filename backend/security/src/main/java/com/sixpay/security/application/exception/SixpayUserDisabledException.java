package com.sixpay.security.application.exception;

public final class SixpayUserDisabledException extends RuntimeException {

    public SixpayUserDisabledException() {
        super("SIXPAY user is disabled");
    }
}
