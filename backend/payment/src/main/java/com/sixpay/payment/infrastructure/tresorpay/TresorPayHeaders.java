package com.sixpay.payment.infrastructure.tresorpay;

public final class TresorPayHeaders {
    public static final String REQUEST_TIMESTAMP = "X-Request-Timestamp";
    public static final String REQUEST_NONCE = "X-Request-Nonce";
    public static final String CALLBACK_SIGNATURE = "X-SIXPAY-Signature";
    private TresorPayHeaders() { }
}
