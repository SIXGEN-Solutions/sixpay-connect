package com.sixpay.customer.verification.domain.exception;

/**
 * Base exception raised when Customer Verification domain invariants are
 * violated.
 */
public final class CustomerVerificationDomainException
        extends RuntimeException {

    public CustomerVerificationDomainException(String message) {
        super(message);
    }
}
