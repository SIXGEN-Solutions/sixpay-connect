package com.sixpay.payment.infrastructure.callback.relay;

import com.sixpay.payment.application.port.out.callback
        .PaymentStatusCallbackDelivery;

record PaymentCallbackPlan(
        boolean deliver,
        PaymentStatusCallbackDelivery delivery
) {

    static PaymentCallbackPlan skip() {
        return new PaymentCallbackPlan(false, null);
    }

    static PaymentCallbackPlan deliver(
            PaymentStatusCallbackDelivery delivery
    ) {
        return new PaymentCallbackPlan(true, delivery);
    }
}
