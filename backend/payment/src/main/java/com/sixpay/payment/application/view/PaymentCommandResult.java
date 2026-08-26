package com.sixpay.payment.application.view;

import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.model.PublicPaymentReference;

import java.util.Objects;

/**
 * Minimal result returned by write use cases.
 */
public record PaymentCommandResult(
        PaymentId paymentId,
        PublicPaymentReference publicPaymentReference,
        PaymentStatus status,
        long businessVersion,
        boolean stateChanged
) {
    public PaymentCommandResult {
        paymentId = Objects.requireNonNull(paymentId, "Payment ID");
        publicPaymentReference = Objects.requireNonNull(
                publicPaymentReference,
                "Public Payment reference"
        );
        status = Objects.requireNonNull(status, "Payment status");

        if (businessVersion <= 0) {
            throw new IllegalArgumentException(
                    "Payment business version must be positive"
            );
        }
    }
}
