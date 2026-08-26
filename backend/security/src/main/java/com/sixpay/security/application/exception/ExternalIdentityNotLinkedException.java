package com.sixpay.security.application.exception;

public final class ExternalIdentityNotLinkedException extends RuntimeException {

    public ExternalIdentityNotLinkedException() {
        super("External identity is not linked to a SIXPAY user");
    }
}
