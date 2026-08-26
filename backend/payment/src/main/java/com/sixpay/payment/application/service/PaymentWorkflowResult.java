package com.sixpay.payment.application.service;

import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.model.PublicPaymentReference;

import java.util.Objects;

/**
 * Stable application result returned by Payment workflow services.
 */
public record PaymentWorkflowResult(
        PaymentId paymentId,
        PublicPaymentReference publicPaymentReference,
        PaymentStatus status,
        long businessVersion,
        boolean stateChanged
) {

    public PaymentWorkflowResult {
        paymentId = Objects.requireNonNull(
                paymentId,
                "Payment ID"
        );
        publicPaymentReference = Objects.requireNonNull(
                publicPaymentReference,
                "Public Payment reference"
        );
        status = Objects.requireNonNull(
                status,
                "Payment status"
        );

        if (businessVersion <= 0) {
            throw new IllegalArgumentException(
                    "Payment business version must be positive"
            );
        }
    }

    static PaymentWorkflowResult from(
            Payment payment,
            boolean stateChanged
    ) {
        return new PaymentWorkflowResult(
                payment.id(),
                payment.publicPaymentReference(),
                payment.status(),
                payment.businessVersion(),
                stateChanged
        );
    }
}
