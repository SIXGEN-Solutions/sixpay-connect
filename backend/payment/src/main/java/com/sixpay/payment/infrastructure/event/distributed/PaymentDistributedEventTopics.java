package com.sixpay.payment.infrastructure.event.distributed;

public final class PaymentDistributedEventTopics {

    public static final String LIFECYCLE =
            "sixpay.payment.lifecycle.v1";
    public static final String FINANCIAL =
            "sixpay.payment.financial.v1";
    public static final String RECONCILIATION =
            "sixpay.payment.reconciliation.v1";

    private PaymentDistributedEventTopics() {
    }
}
