package com.sixpay.payment.api;

import java.util.UUID;

public final class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(UUID paymentId) {
        super("Payment not found: " + paymentId);
    }

    public PaymentNotFoundException(String paymentReference) {
        super("Payment not found: " + paymentReference);
    }
}
