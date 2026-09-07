package com.sixpay.accounting.application.port.output;

import com.sixpay.accounting.domain.model.TresorPayPaymentStatusEvidence;

/**
 * Accounting-owned provider-neutral boundary for the authoritative
 * TRESOR PAY payment-status lookup.
 *
 * <p>No provider implementation is authorized by T1.1.</p>
 */
public interface TresorPayPaymentStatusGateway {

    TresorPayPaymentStatusEvidence findByPaymentReference(
            String paymentReference,
            AccountingIntegrationContext context
    );
}
