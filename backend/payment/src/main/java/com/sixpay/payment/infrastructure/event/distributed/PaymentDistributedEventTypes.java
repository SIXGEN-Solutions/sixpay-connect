package com.sixpay.payment.infrastructure.event.distributed;

public final class PaymentDistributedEventTypes {

    public static final String PAYMENT_RECEIVED =
            "payment.received.v1";
    public static final String PAYMENT_POSTED =
            "payment.posted.v1";
    public static final String PAYMENT_REVERSED =
            "payment.reversed.v1";
    public static final String PAYMENT_FAILED =
            "payment.failed.v1";
    public static final String
            PAYMENT_RECONCILIATION_REQUIRED =
            "payment.reconciliation-required.v1";

    private PaymentDistributedEventTypes() {
    }
}
