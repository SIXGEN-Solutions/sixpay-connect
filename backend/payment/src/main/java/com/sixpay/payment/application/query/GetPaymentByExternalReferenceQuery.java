package com.sixpay.payment.application.query;

import com.sixpay.payment.domain.model.ExternalPaymentReference;
import com.sixpay.payment.domain.model.PaymentSource;

import java.util.Objects;

public record GetPaymentByExternalReferenceQuery(
        PaymentSource source,
        ExternalPaymentReference externalPaymentReference
) {
    public GetPaymentByExternalReferenceQuery {
        source = Objects.requireNonNull(
                source,
                "Payment source"
        );
        externalPaymentReference = Objects.requireNonNull(
                externalPaymentReference,
                "External Payment reference"
        );
    }
}
