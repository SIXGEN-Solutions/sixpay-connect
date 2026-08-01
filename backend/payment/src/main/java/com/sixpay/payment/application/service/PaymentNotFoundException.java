package com.sixpay.payment.application.service;

import com.sixpay.payment.domain.model.PaymentId;

/**
 * Raised when an application workflow targets an unknown Payment.
 */
public final class PaymentNotFoundException
        extends RuntimeException {

    public PaymentNotFoundException(PaymentId paymentId) {
        super("Payment not found: " + paymentId);
    }
}
