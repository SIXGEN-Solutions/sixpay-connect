package com.sixpay.security.application.exception;

public final class LdapProvisioningConflictException extends RuntimeException {
    public enum Reason {
        ALREADY_PROVISIONED,
        USERNAME_CONFLICT
    }

    private final Reason reason;

    public LdapProvisioningConflictException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
