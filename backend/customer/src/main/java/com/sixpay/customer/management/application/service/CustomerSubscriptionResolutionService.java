package com.sixpay.customer.management.application.service;

import com.sixpay.customer.management.application.port.input.ResolveCustomerSubscriptionUseCase;
import com.sixpay.customer.management.application.port.input.ResolvedCustomerSubscription;
import com.sixpay.customer.management.domain.exception.CustomerDomainException;
import com.sixpay.customer.management.domain.model.CustomerSubscriptionId;
import com.sixpay.customer.management.domain.model.CustomerStatus;
import com.sixpay.customer.management.domain.repository.CustomerRepository;
import com.sixpay.customer.management.domain.repository.CustomerSubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

/** In-process Customer boundary: no Payment or Partner persistence dependencies. */
@Service
@Transactional(readOnly = true)
public class CustomerSubscriptionResolutionService implements ResolveCustomerSubscriptionUseCase {
    private final CustomerSubscriptionRepository subscriptions;
    private final CustomerRepository customers;

    public CustomerSubscriptionResolutionService(
            CustomerSubscriptionRepository subscriptions, CustomerRepository customers) {
        this.subscriptions = Objects.requireNonNull(subscriptions);
        this.customers = Objects.requireNonNull(customers);
    }

    @Override
    public ResolvedCustomerSubscription resolve(UUID referenceClient, UUID authenticatedPartnerId) {
        Objects.requireNonNull(referenceClient, "referenceClient is required");
        Objects.requireNonNull(authenticatedPartnerId, "authenticatedPartnerId is required");
        var subscription = subscriptions.findById(new CustomerSubscriptionId(referenceClient))
                .orElseThrow(() -> new CustomerDomainException("customer subscription unavailable"));
        // Do not disclose existence of a subscription belonging to a different partner.
        if (!authenticatedPartnerId.equals(subscription.partnerId()) || !subscription.acceptsPayments()) {
            throw new CustomerDomainException("customer subscription unavailable");
        }
        var customer = customers.findById(subscription.customerId())
                .orElseThrow(() -> new CustomerDomainException("customer subscription unavailable"));
        if (customer.status() != CustomerStatus.ACTIVE) {
            throw new CustomerDomainException("customer subscription unavailable");
        }
        var account = customer.bankAccounts().stream()
                .filter(candidate -> candidate.id().equals(subscription.bankAccountId()))
                .findFirst()
                .orElseThrow(() -> new CustomerDomainException("customer subscription unavailable"));
        return new ResolvedCustomerSubscription(
                subscription.id().value(), customer.id().value(), subscription.partnerId(),
                account.id().value(), customer.financialInstitutionCode(),
                customer.bankingCustomerReference(), customer.customerNumber().orElse(null),
                customer.niu().orElse(null), customer.legalName(),
                account.bankingAccountReference(), account.accountBindingFingerprint(),
                account.maskedAccountIdentifier());
    }
}
