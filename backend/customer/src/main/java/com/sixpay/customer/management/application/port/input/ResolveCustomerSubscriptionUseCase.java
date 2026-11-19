package com.sixpay.customer.management.application.port.input;

import java.util.UUID;

/** Resolve CustomerSubscription in a trusted Partner context. */
public interface ResolveCustomerSubscriptionUseCase {
    /** Existing internal UUID-based resolution remains unchanged. */
    ResolvedCustomerSubscription resolve(UUID referenceClient, UUID authenticatedPartnerId);

    /** Resolve the banking Customer reference without exposing subscription IDs. */
    ResolvedCustomerSubscription resolveByBankingCustomerReference(
            String bankingCustomerReference, UUID authenticatedPartnerId);
}
