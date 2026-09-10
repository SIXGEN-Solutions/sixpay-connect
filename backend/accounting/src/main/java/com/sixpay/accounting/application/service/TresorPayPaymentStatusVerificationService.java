package com.sixpay.accounting.application.service;

import com.sixpay.accounting.application.port.output.AccountingIntegrationContext;
import com.sixpay.accounting.application.port.output.TresorPayPaymentStatusGateway;
import com.sixpay.accounting.domain.model.TresorPayPaymentStatusEvidence;

import java.util.Objects;

public final class TresorPayPaymentStatusVerificationService {

    private final TresorPayPaymentStatusGateway gateway;

    public TresorPayPaymentStatusVerificationService(
            TresorPayPaymentStatusGateway gateway
    ) {
        this.gateway = Objects.requireNonNull(gateway, "gateway");
    }

    public TresorPayPaymentStatusEvidence verify(
            String paymentReference,
            AccountingIntegrationContext context
    ) {
        if (paymentReference == null || paymentReference.isBlank()) {
            throw new IllegalArgumentException("paymentReference is required");
        }

        String normalized = paymentReference.strip();
        TresorPayPaymentStatusEvidence evidence =
                Objects.requireNonNull(
                        gateway.findByPaymentReference(
                                normalized,
                                Objects.requireNonNull(context, "context")
                        ),
                        "TRESOR PAY status evidence"
                );

        if (!normalized.equals(evidence.reference())) {
            throw new IllegalStateException(
                    "TRESOR PAY status response reference mismatch"
            );
        }

        return evidence;
    }
}
