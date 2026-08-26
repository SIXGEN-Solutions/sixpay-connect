package com.sixpay.payment.application.security;

import org.springframework.security.access.AccessDeniedException;

public final class PaymentAccessDeniedException
        extends AccessDeniedException {

    public PaymentAccessDeniedException(String message) {
        super(message);
    }
}
