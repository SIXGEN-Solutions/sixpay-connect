package com.sixpay.payment.application.query;

import com.sixpay.payment.domain.model.PublicPaymentReference;

import java.util.Objects;

public record GetPaymentByPublicReferenceQuery(
        PublicPaymentReference publicPaymentReference
) {
    public GetPaymentByPublicReferenceQuery {
        publicPaymentReference = Objects.requireNonNull(
                publicPaymentReference,
                "Public Payment reference"
        );
    }
}
