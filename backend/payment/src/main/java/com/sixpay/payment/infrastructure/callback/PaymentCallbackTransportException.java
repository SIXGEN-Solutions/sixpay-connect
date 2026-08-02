package com.sixpay.payment.infrastructure.callback;

public final class PaymentCallbackTransportException
        extends RuntimeException {

    public PaymentCallbackTransportException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
