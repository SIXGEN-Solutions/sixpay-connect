package com.sixpay.payment.infrastructure.event.distributed;

import java.util.Map;

public final class PaymentEventTopicRoutes {

    private PaymentEventTopicRoutes() {
    }

    public static Map<String, String> defaults() {
        return Map.of(
                PaymentDistributedEventTypes.PAYMENT_RECEIVED,
                PaymentDistributedEventTopics.LIFECYCLE,
                PaymentDistributedEventTypes.PAYMENT_FAILED,
                PaymentDistributedEventTopics.LIFECYCLE,
                PaymentDistributedEventTypes.PAYMENT_POSTED,
                PaymentDistributedEventTopics.FINANCIAL,
                PaymentDistributedEventTypes.PAYMENT_REVERSED,
                PaymentDistributedEventTopics.FINANCIAL,
                PaymentDistributedEventTypes
                        .PAYMENT_RECONCILIATION_REQUIRED,
                PaymentDistributedEventTopics.RECONCILIATION
        );
    }
}
