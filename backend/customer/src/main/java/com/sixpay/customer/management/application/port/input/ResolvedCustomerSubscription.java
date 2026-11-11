package com.sixpay.customer.management.application.port.input;

import java.util.UUID;

/** Customer-owned, application-level view for an already eligible subscription. */
public record ResolvedCustomerSubscription(
        UUID subscriptionId,
        UUID customerId,
        UUID partnerId,
        UUID bankAccountId,
        String financialInstitutionCode,
        String bankingCustomerReference,
        String customerNumber,
        String niu,
        String legalName,
        String bankingAccountReference,
        String accountBindingFingerprint,
        String maskedAccountIdentifier
) {
}
