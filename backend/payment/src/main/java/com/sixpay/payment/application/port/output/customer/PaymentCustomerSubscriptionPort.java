package com.sixpay.payment.application.port.output.customer;

import java.util.UUID;

/** Payment-to-Customer application boundary. Customer owns subscription data. */
public interface PaymentCustomerSubscriptionPort {
    ResolvedPaymentSubscription resolve(UUID referenceClient, UUID authenticatedPartnerId);

    /** Resolve the banking reference to an active Customer-owned subscription. */
    ResolvedPaymentSubscription resolveByBankingCustomerReference(
            String bankingCustomerReference, UUID authenticatedPartnerId);
}
