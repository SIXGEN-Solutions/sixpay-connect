package com.sixpay.payment.application.service;

/**
 * Raised when a previous InitiateDebit execution is still in progress.
 */
public final class PaymentInitiationInProgressException
        extends RuntimeException {

    public PaymentInitiationInProgressException() {
        super("Payment initiation is already in progress");
    }
}
