package com.sixpay.payment.infrastructure.web;

import java.util.UUID;

public final class PaymentProjectionNotFoundException extends RuntimeException {
    public PaymentProjectionNotFoundException(UUID paymentId) {
        super("Payment projection not found: " + paymentId);
    }
}
