package com.sixpay.customer.management.subscription;

import com.sixpay.customer.management.application.service.CustomerSubscriptionResolutionService;
import com.sixpay.customer.management.domain.exception.CustomerDomainException;
import com.sixpay.customer.management.domain.model.*;
import com.sixpay.customer.management.domain.repository.CustomerRepository;
import com.sixpay.customer.management.domain.repository.CustomerSubscriptionRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomerSubscriptionResolutionTest {
    private static final Instant NOW = Instant.parse("2026-08-22T20:00:00Z");
    private final CustomerRepository customers = mock(CustomerRepository.class);
    private final CustomerSubscriptionRepository subscriptions = mock(CustomerSubscriptionRepository.class);
    private final CustomerSubscriptionResolutionService service =
            new CustomerSubscriptionResolutionService(subscriptions, customers);
    private final UUID partnerId = UUID.randomUUID();
    private final CustomerId customerId = new CustomerId(UUID.randomUUID());
    private final CustomerBankAccountId accountId = new CustomerBankAccountId(UUID.randomUUID());
    private final CustomerSubscriptionId subscriptionId = new CustomerSubscriptionId(UUID.randomUUID());

    @Test
    void resolvesActiveSubscriptionAndReturnsBankingEvidenceWithOptionalNiu() {
        var subscription = activeSubscription();
        when(subscriptions.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(customers.findById(customerId)).thenReturn(Optional.of(customer(null)));
        var resolved = service.resolve(subscriptionId.value(), partnerId);
        assertThat(resolved.subscriptionId()).isEqualTo(subscriptionId.value());
        assertThat(resolved.niu()).isNull();
        assertThat(resolved.customerNumber()).isEqualTo("000123");
        assertThat(resolved.bankingAccountReference()).isEqualTo("ACC-001");
        assertThat(resolved.bankAccountId()).isEqualTo(accountId.value());
    }

    @Test
    void rejectsCrossPartnerLookupBeforeLoadingCustomer() {
        when(subscriptions.findById(subscriptionId)).thenReturn(Optional.of(activeSubscription()));
        assertThatThrownBy(() -> service.resolve(subscriptionId.value(), UUID.randomUUID()))
                .isInstanceOf(CustomerDomainException.class);
        verifyNoInteractions(customers);
    }

    @Test
    void rejectsPendingSubscription() {
        when(subscriptions.findById(subscriptionId)).thenReturn(Optional.of(pendingSubscription()));
        assertThatThrownBy(() -> service.resolve(subscriptionId.value(), partnerId))
                .isInstanceOf(CustomerDomainException.class);
        verifyNoInteractions(customers);
    }

    @Test
    void rejectsAbsentSubscription() {
        when(subscriptions.findById(subscriptionId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.resolve(subscriptionId.value(), partnerId))
                .isInstanceOf(CustomerDomainException.class);
    }

    @Test
    void rejectsMissingAssociatedAccount() {
        when(subscriptions.findById(subscriptionId)).thenReturn(Optional.of(activeSubscription()));
        CustomerId id = customerId;
        var otherAccount = CustomerBankAccount.create(
                new CustomerBankAccountId(UUID.randomUUID()), id, "OTHER-ACC", "v1:" + "a".repeat(64),
                "****0001", "XAF", "CURRENT", NOW);
        var customer = Customer.create(id, "BANK", "BANK-CUST-001", "000123", "NIU-001",
                "Customer One", null, null, otherAccount, NOW);
        when(customers.findById(customerId)).thenReturn(Optional.of(customer));
        assertThatThrownBy(() -> service.resolve(subscriptionId.value(), partnerId))
                .isInstanceOf(CustomerDomainException.class);
    }

    private CustomerSubscription pendingSubscription() {
        return CustomerSubscription.create(subscriptionId, customerId, partnerId, accountId, NOW);
    }

    private CustomerSubscription activeSubscription() {
        var subscription = pendingSubscription();
        subscription.activate(NOW.plusSeconds(1));
        return subscription;
    }

    private Customer customer(String niu) {
        var account = CustomerBankAccount.create(accountId, customerId, "ACC-001",
                "v1:" + "a".repeat(64), "****0001", "XAF", "CURRENT", NOW);
        return Customer.create(customerId, "BANK", "BANK-CUST-001", "000123", niu,
                "Customer One", null, null, account, NOW);
    }
}
