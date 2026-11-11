package com.sixpay.customer.management.application.port.input;

import java.util.UUID;

/** Resolve a referenceClient (the CustomerSubscription ID) in a trusted partner context. */
public interface ResolveCustomerSubscriptionUseCase {
    ResolvedCustomerSubscription resolve(UUID referenceClient, UUID authenticatedPartnerId);
}
