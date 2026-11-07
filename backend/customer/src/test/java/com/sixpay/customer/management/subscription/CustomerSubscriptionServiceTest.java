package com.sixpay.customer.management.subscription;

import com.sixpay.customer.management.application.port.output.CustomerEnrollmentIdGenerator;
import com.sixpay.customer.management.application.port.output.PartnerSubscriptionEligibilityPort;
import com.sixpay.customer.management.application.service.CustomerSubscriptionService;
import com.sixpay.customer.management.domain.exception.CustomerDomainException;
import com.sixpay.customer.management.domain.model.*;
import com.sixpay.customer.management.domain.repository.CustomerRepository;
import com.sixpay.customer.management.domain.repository.CustomerSubscriptionRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class CustomerSubscriptionServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-08-22T20:00:00Z");

    @Test
    void createsPendingSubscriptionOnlyForActiveCustomerPartnerAndOwnedAccount() {
        CustomerRepository customers =
                mock(CustomerRepository.class);
        CustomerSubscriptionRepository subscriptions =
                mock(CustomerSubscriptionRepository.class);
        PartnerSubscriptionEligibilityPort partners =
                mock(PartnerSubscriptionEligibilityPort.class);
        CustomerEnrollmentIdGenerator ids =
                mock(CustomerEnrollmentIdGenerator.class);

        Customer customer = customer();

        when(customers.findById(customer.id()))
                .thenReturn(Optional.of(customer));
        when(partners.check(any()))
                .thenReturn(
                        new PartnerSubscriptionEligibilityPort
                                .PartnerEligibility(
                                        true,
                                        true
                                )
                );
        when(ids.nextId())
                .thenReturn(UUID.randomUUID());
        when(subscriptions.save(any()))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        CustomerSubscriptionService service =
                new CustomerSubscriptionService(
                        customers,
                        subscriptions,
                        partners,
                        ids
                );

        CustomerSubscription subscription =
                service.create(
                        customer.id(),
                        UUID.randomUUID(),
                        customer.defaultBankAccount()
                                .orElseThrow()
                                .id(),
                        NOW
                );

        assertThat(subscription.status())
                .isEqualTo(
                        CustomerSubscriptionStatus.PENDING_ACTIVATION
                );

        verify(subscriptions).save(subscription);
    }

    @Test
    void refusesInactivePartner() {
        CustomerRepository customers =
                mock(CustomerRepository.class);
        CustomerSubscriptionRepository subscriptions =
                mock(CustomerSubscriptionRepository.class);
        PartnerSubscriptionEligibilityPort partners =
                mock(PartnerSubscriptionEligibilityPort.class);
        CustomerEnrollmentIdGenerator ids =
                mock(CustomerEnrollmentIdGenerator.class);

        Customer customer = customer();

        when(customers.findById(customer.id()))
                .thenReturn(Optional.of(customer));
        when(partners.check(any()))
                .thenReturn(
                        new PartnerSubscriptionEligibilityPort
                                .PartnerEligibility(
                                        true,
                                        false
                                )
                );

        CustomerSubscriptionService service =
                new CustomerSubscriptionService(
                        customers,
                        subscriptions,
                        partners,
                        ids
                );

        assertThatThrownBy(() ->
                service.create(
                        customer.id(),
                        UUID.randomUUID(),
                        customer.defaultBankAccount()
                                .orElseThrow()
                                .id(),
                        NOW
                )
        ).isInstanceOf(CustomerDomainException.class)
                .hasMessageContaining(
                        "partner is not active"
                );

        verify(subscriptions, never()).save(any());
    }


    @Test
    void usesDefaultBankAccountWhenNotProvidedAndKeepsPendingActivation() {
        CustomerRepository customers = mock(CustomerRepository.class);
        CustomerSubscriptionRepository subscriptions = mock(CustomerSubscriptionRepository.class);
        PartnerSubscriptionEligibilityPort partners = mock(PartnerSubscriptionEligibilityPort.class);
        CustomerEnrollmentIdGenerator ids = mock(CustomerEnrollmentIdGenerator.class);
        Customer customer = customer();
        UUID partnerId = UUID.randomUUID();
        when(customers.findById(customer.id())).thenReturn(Optional.of(customer));
        when(partners.check(partnerId)).thenReturn(
                new PartnerSubscriptionEligibilityPort.PartnerEligibility(true, true));
        when(ids.nextId()).thenReturn(UUID.randomUUID());
        when(subscriptions.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var service = new CustomerSubscriptionService(customers, subscriptions, partners, ids);

        var subscription = service.create(customer.id(), partnerId, null, NOW);

        assertThat(subscription.bankAccountId())
                .isEqualTo(customer.defaultBankAccount().orElseThrow().id());
        assertThat(subscription.status()).isEqualTo(CustomerSubscriptionStatus.PENDING_ACTIVATION);
        assertThat(subscription.activatedAt()).isEmpty();
        verify(subscriptions).save(subscription);
    }

    @Test
    void rejectsExplicitBankAccountNotOwnedByCustomer() {
        CustomerRepository customers = mock(CustomerRepository.class);
        CustomerSubscriptionRepository subscriptions = mock(CustomerSubscriptionRepository.class);
        PartnerSubscriptionEligibilityPort partners = mock(PartnerSubscriptionEligibilityPort.class);
        CustomerEnrollmentIdGenerator ids = mock(CustomerEnrollmentIdGenerator.class);
        Customer customer = customer();
        when(customers.findById(customer.id())).thenReturn(Optional.of(customer));
        var service = new CustomerSubscriptionService(customers, subscriptions, partners, ids);

        assertThatThrownBy(() -> service.create(customer.id(), UUID.randomUUID(),
                new CustomerBankAccountId(UUID.randomUUID()), NOW))
                .isInstanceOf(CustomerDomainException.class)
                .hasMessageContaining("does not belong to customer");
        verifyNoInteractions(partners);
        verify(subscriptions, never()).save(any());
    }

    private static Customer customer() {
        CustomerId id =
                new CustomerId(UUID.randomUUID());

        return Customer.create(
                id,
                "SIXPAY_BANK",
                "BANK-CUSTOMER-001",
                "000123",
                "NIU-001",
                "Customer One",
                "customer@example.com",
                "+237600000001",
                CustomerBankAccount.create(
                        new CustomerBankAccountId(
                                UUID.randomUUID()
                        ),
                        id,
                        "ACC-001",
                        "v1:" + "a".repeat(64),
                        "****0001",
                        "XAF",
                        "CURRENT",
                        NOW
                ),
                NOW
        );
    }
}
