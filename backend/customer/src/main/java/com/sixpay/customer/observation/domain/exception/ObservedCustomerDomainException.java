package com.sixpay.customer.observation.domain.exception;

public final class ObservedCustomerDomainException extends RuntimeException {
    public ObservedCustomerDomainException(String message) {
        super(message);
    }

    public ObservedCustomerDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
