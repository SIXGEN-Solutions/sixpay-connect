package com.sixpay.accounting.application.port.output;

import com.sixpay.accounting.domain.model.PartnerExternalPaymentStatusEvidence;

/**
 * Accounting-owned provider-neutral boundary for the authoritative
 * Partner payment-status lookup.
 *
 * <p>The T1.1 provider implementation is authorized by the active
 * Partner status-query contract.</p>
 */
public interface PartnerExternalPaymentStatusGateway {

    PartnerExternalPaymentStatusEvidence findByPaymentReference(
            String paymentReference,
            AccountingIntegrationContext context
    );
}
