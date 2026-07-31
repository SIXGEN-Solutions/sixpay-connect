package com.sixpay.payment.domain.policy;

import com.sixpay.payment.domain.model.PublicPaymentReference;

import java.util.Objects;

public record PaymentResultContext(
        PublicPaymentReference publicPaymentReference,
        String correlationReference
) {
    public PaymentResultContext {
        publicPaymentReference = Objects.requireNonNull(
                publicPaymentReference,
                "Public Payment reference"
        );
        Objects.requireNonNull(
                correlationReference,
                "Correlation reference"
        );
        correlationReference = correlationReference.strip();
        if (correlationReference.isEmpty()) {
            throw new IllegalArgumentException(
                    "Correlation reference must not be blank"
            );
        }
    }
}
