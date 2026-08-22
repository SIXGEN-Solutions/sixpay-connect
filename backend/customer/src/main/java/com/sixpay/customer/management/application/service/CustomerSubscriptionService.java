package com.sixpay.customer.management.application.service;

import com.sixpay.customer.management.application.port.input.CustomerSubscriptionUseCase;
import com.sixpay.customer.management.application.port.output.CustomerEnrollmentIdGenerator;
import com.sixpay.customer.management.application.port.output.PartnerSubscriptionEligibilityPort;
import com.sixpay.customer.management.domain.exception.CustomerDomainException;
import com.sixpay.customer.management.domain.model.Customer;
import com.sixpay.customer.management.domain.model.CustomerBankAccountId;
import com.sixpay.customer.management.domain.model.CustomerId;
import com.sixpay.customer.management.domain.model.CustomerSubscription;
import com.sixpay.customer.management.domain.model.CustomerSubscriptionId;
import com.sixpay.customer.management.domain.repository.CustomerRepository;
import com.sixpay.customer.management.domain.repository.CustomerSubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public final class CustomerSubscriptionService
        implements CustomerSubscriptionUseCase {

    private final CustomerRepository customerRepository;
    private final CustomerSubscriptionRepository subscriptionRepository;
    private final PartnerSubscriptionEligibilityPort partnerEligibility;
    private final CustomerEnrollmentIdGenerator idGenerator;

    public CustomerSubscriptionService(
            CustomerRepository customerRepository,
            CustomerSubscriptionRepository subscriptionRepository,
            PartnerSubscriptionEligibilityPort partnerEligibility,
            CustomerEnrollmentIdGenerator idGenerator
    ) {
        this.customerRepository =
                Objects.requireNonNull(customerRepository);
        this.subscriptionRepository =
                Objects.requireNonNull(subscriptionRepository);
        this.partnerEligibility =
                Objects.requireNonNull(partnerEligibility);
        this.idGenerator =
                Objects.requireNonNull(idGenerator);
    }

    @Override
    public CustomerSubscription create(
            CustomerId customerId,
            UUID partnerId,
            CustomerBankAccountId bankAccountId,
            Instant now
    ) {
        Customer customer = loadCustomer(customerId);

        requireCustomerEligible(customer);
        requireAccountBelongsToCustomer(customer, bankAccountId);
        requirePartnerActive(partnerId);

        if (subscriptionRepository
                .existsOpenByCustomerIdAndPartnerId(
                        customerId,
                        partnerId
                )) {
            throw new CustomerDomainException(
                    "an open subscription already exists "
                            + "for customer and partner"
            );
        }

        CustomerSubscription subscription =
                CustomerSubscription.create(
                        new CustomerSubscriptionId(
                                idGenerator.nextId()
                        ),
                        customerId,
                        partnerId,
                        bankAccountId,
                        now
                );

        return subscriptionRepository.save(subscription);
    }

    @Override
    public CustomerSubscription activate(
            CustomerSubscriptionId subscriptionId,
            Instant now
    ) {
        CustomerSubscription subscription =
                loadSubscription(subscriptionId);

        Customer customer =
                loadCustomer(subscription.customerId());

        requireCustomerEligible(customer);
        requireAccountBelongsToCustomer(
                customer,
                subscription.bankAccountId()
        );
        requirePartnerActive(subscription.partnerId());

        subscription.activate(now);

        return subscriptionRepository.save(subscription);
    }

    @Override
    public CustomerSubscription suspend(
            CustomerSubscriptionId subscriptionId,
            String reason,
            Instant now
    ) {
        CustomerSubscription subscription =
                loadSubscription(subscriptionId);

        subscription.suspend(reason, now);

        return subscriptionRepository.save(subscription);
    }

    @Override
    public CustomerSubscription close(
            CustomerSubscriptionId subscriptionId,
            String reason,
            Instant now
    ) {
        CustomerSubscription subscription =
                loadSubscription(subscriptionId);

        subscription.close(reason, now);

        return subscriptionRepository.save(subscription);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerSubscription findById(
            CustomerSubscriptionId subscriptionId
    ) {
        return loadSubscription(subscriptionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerSubscription> findByCustomerId(
            CustomerId customerId
    ) {
        return subscriptionRepository.findByCustomerId(
                customerId
        );
    }

    private Customer loadCustomer(CustomerId customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() ->
                        new CustomerDomainException(
                                "customer not found: "
                                        + customerId
                        )
                );
    }

    private CustomerSubscription loadSubscription(
            CustomerSubscriptionId subscriptionId
    ) {
        return subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() ->
                        new CustomerDomainException(
                                "subscription not found: "
                                        + subscriptionId
                        )
                );
    }

    private static void requireCustomerEligible(
            Customer customer
    ) {
        if (!customer.acceptsNewSubscriptions()) {
            throw new CustomerDomainException(
                    "customer is not eligible for subscription"
            );
        }
    }

    private static void requireAccountBelongsToCustomer(
            Customer customer,
            CustomerBankAccountId accountId
    ) {
        boolean belongs =
                customer.bankAccounts()
                        .stream()
                        .anyMatch(account ->
                                account.id()
                                        .equals(accountId)
                        );

        if (!belongs) {
            throw new CustomerDomainException(
                    "subscription account does not belong to customer"
            );
        }
    }

    private void requirePartnerActive(UUID partnerId) {
        var eligibility = partnerEligibility.check(partnerId);

        if (!eligibility.exists()) {
            throw new CustomerDomainException(
                    "partner not found: " + partnerId
            );
        }

        if (!eligibility.active()) {
            throw new CustomerDomainException(
                    "partner is not active: " + partnerId
            );
        }
    }
}
