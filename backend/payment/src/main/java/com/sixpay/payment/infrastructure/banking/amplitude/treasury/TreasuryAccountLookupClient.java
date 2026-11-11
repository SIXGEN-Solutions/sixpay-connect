package com.sixpay.payment.infrastructure.banking.amplitude.treasury;

import com.sixpay.common.context.CorrelationId;

/** Bank-native lookup; no payment-domain decisions in this client. */
public interface TreasuryAccountLookupClient {
    TreasuryAccountLookupResponse lookup(
            String branchId,
            String accountNumber,
            CorrelationId correlationId
    );
}
