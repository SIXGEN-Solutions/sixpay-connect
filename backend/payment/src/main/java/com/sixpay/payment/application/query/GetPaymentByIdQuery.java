package com.sixpay.payment.application.query;

import com.sixpay.payment.domain.model.PaymentId;

import java.util.Objects;

public record GetPaymentByIdQuery(PaymentId paymentId) {
    public GetPaymentByIdQuery {
        paymentId = Objects.requireNonNull(paymentId, "Payment ID");
    }
}
