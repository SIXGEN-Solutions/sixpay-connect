package com.sixpay.payment.application.port.output.initiation;

import com.sixpay.payment.domain.model.NewPaymentIntent;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PublicPaymentReference;

import java.time.Instant;
import java.util.Objects;

public record PreparedPaymentInitiation(
        PaymentId paymentId,
        PublicPaymentReference publicPaymentReference,
        NewPaymentIntent intent,
        Instant receivedAt
) {
    public PreparedPaymentInitiation {
        paymentId = Objects.requireNonNull(paymentId);
        publicPaymentReference = Objects.requireNonNull(publicPaymentReference);
        intent = Objects.requireNonNull(intent);
        receivedAt = Objects.requireNonNull(receivedAt);
    }
}
