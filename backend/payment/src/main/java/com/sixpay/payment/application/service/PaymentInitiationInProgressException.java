package com.sixpay.payment.application.service;

/**
 * Raised when a previous InitiateDebit execution is still input progress.
 */
public final class PaymentInitiationInProgressException
        extends RuntimeException {

    public PaymentInitiationInProgressException() {
        super("Payment initiation is already input progress");
    }
}
