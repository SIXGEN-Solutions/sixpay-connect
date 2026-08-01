package com.sixpay.payment.application.command;

import com.sixpay.payment.domain.model.NewPaymentIntent;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PublicPaymentReference;

import java.time.Instant;
import java.util.Objects;

public record ReceivePaymentCommand(
        PaymentId paymentId,
        PublicPaymentReference publicPaymentReference,
        NewPaymentIntent intent,
        Instant receivedAt
) {
    public ReceivePaymentCommand {
        paymentId = Objects.requireNonNull(paymentId, "Payment ID");
        publicPaymentReference = Objects.requireNonNull(
                publicPaymentReference,
                "Public Payment reference"
        );
        intent = Objects.requireNonNull(intent, "New Payment intent");
        receivedAt = Objects.requireNonNull(
                receivedAt,
                "Received instant"
        );
    }
}
