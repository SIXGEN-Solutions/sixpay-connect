package com.sixpay.bootstrap.integration.customer;

import com.sixpay.customer.management.application.port.input.ResolveCustomerSubscriptionUseCase;
import com.sixpay.customer.management.application.port.input.ResolvedCustomerSubscription;
import com.sixpay.customer.management.domain.exception.CustomerDomainException;
import com.sixpay.payment.application.port.output.customer.PaymentCustomerSubscriptionPort;
import com.sixpay.payment.application.port.output.customer.ResolvedPaymentSubscription;
import java.util.Objects;
import java.util.UUID;

/** Composition only: delegates authorization and ownership checks to Customer. */
public final class PaymentCustomerSubscriptionModuleAdapter
        implements PaymentCustomerSubscriptionPort {
    private final ResolveCustomerSubscriptionUseCase customer;

    public PaymentCustomerSubscriptionModuleAdapter(ResolveCustomerSubscriptionUseCase customer) {
        this.customer = Objects.requireNonNull(customer);
    }

    @Override
    public ResolvedPaymentSubscription resolve(UUID referenceClient, UUID authenticatedPartnerId) {
        return toPaymentSubscription(customer.resolve(referenceClient, authenticatedPartnerId));
    }

    @Override
    public ResolvedPaymentSubscription resolveByBankingCustomerReference(
            String bankingCustomerReference, UUID authenticatedPartnerId) {
        try {
            return toPaymentSubscription(customer.resolveByBankingCustomerReference(
                    bankingCustomerReference, authenticatedPartnerId));
        } catch (CustomerDomainException unavailable) {
            // Keep the existing Payment API's generic 400 response and reveal no ownership data.
            throw new IllegalArgumentException("customer subscription unavailable", unavailable);
        }
    }

    private static ResolvedPaymentSubscription toPaymentSubscription(
            ResolvedCustomerSubscription subscription) {
        return new ResolvedPaymentSubscription(
                subscription.subscriptionId(), subscription.partnerId(),
                subscription.financialInstitutionCode(), subscription.bankingCustomerReference(),
                subscription.customerNumber(), subscription.niu(), subscription.legalName(),
                subscription.bankingAccountReference(), subscription.accountBindingFingerprint(),
                subscription.maskedAccountIdentifier());
    }
}
