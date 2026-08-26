package com.sixpay.payment.infrastructure.callback;

public final class PaymentCallbackSigningException
        extends RuntimeException {

    public PaymentCallbackSigningException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
