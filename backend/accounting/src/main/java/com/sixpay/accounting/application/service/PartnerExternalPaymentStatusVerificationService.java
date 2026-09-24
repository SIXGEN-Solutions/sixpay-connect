package com.sixpay.accounting.application.service;

import com.sixpay.accounting.application.port.output.AccountingIntegrationContext;
import com.sixpay.accounting.application.port.output.PartnerExternalPaymentStatusGateway;
import com.sixpay.accounting.domain.model.PartnerExternalPaymentStatusEvidence;

import java.util.Objects;

public final class PartnerExternalPaymentStatusVerificationService {

    private final PartnerExternalPaymentStatusGateway gateway;

    public PartnerExternalPaymentStatusVerificationService(
            PartnerExternalPaymentStatusGateway gateway
    ) {
        this.gateway = Objects.requireNonNull(gateway, "gateway");
    }

    public PartnerExternalPaymentStatusEvidence verify(
            String paymentReference,
            AccountingIntegrationContext context
    ) {
        if (paymentReference == null || paymentReference.isBlank()) {
            throw new IllegalArgumentException("paymentReference is required");
        }

        String normalized = paymentReference.strip();
        PartnerExternalPaymentStatusEvidence evidence =
                Objects.requireNonNull(
                        gateway.findByPaymentReference(
                                normalized,
                                Objects.requireNonNull(context, "context")
                        ),
                        "Partner external status evidence"
                );

        if (!normalized.equals(evidence.reference())) {
            throw new IllegalStateException(
                    "Partner external status response reference mismatch"
            );
        }

        return evidence;
    }
}
