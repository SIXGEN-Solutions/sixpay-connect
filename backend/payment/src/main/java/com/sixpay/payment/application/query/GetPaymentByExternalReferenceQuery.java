package com.sixpay.payment.application.query;

import com.sixpay.payment.domain.model.ExternalPaymentReference;
import com.sixpay.payment.domain.model.CanonicalPartnerIdentity;

import java.util.Objects;

public record GetPaymentByExternalReferenceQuery(
        CanonicalPartnerIdentity partnerIdentity,
        ExternalPaymentReference externalPaymentReference
) {
    public GetPaymentByExternalReferenceQuery {
        partnerIdentity = Objects.requireNonNull(
                partnerIdentity,
                "Canonical Partner identity"
        );
        externalPaymentReference = Objects.requireNonNull(
                externalPaymentReference,
                "External Payment reference"
        );
    }
}
