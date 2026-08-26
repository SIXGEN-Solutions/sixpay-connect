package com.sixpay.security.application.exception;

/**
 * Raised when an authenticated user cannot prove knowledge of the current
 * LOCAL password before replacing it.
 */
public final class CurrentPasswordMismatchException
        extends RuntimeException {

    public CurrentPasswordMismatchException() {
        super("Current password is invalid");
    }
}
