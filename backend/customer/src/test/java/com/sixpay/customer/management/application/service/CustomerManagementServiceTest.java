package com.sixpay.customer.management.application.service;

import com.sixpay.customer.management.application.port.input.AddBankAccountCommand;
import com.sixpay.customer.management.application.port.output.BankingCustomerLookupPort;
import com.sixpay.customer.management.application.port.output.CustomerEnrollmentIdGenerator;
import com.sixpay.customer.management.domain.exception.CustomerDomainException;
import com.sixpay.customer.management.domain.model.Customer;
import com.sixpay.customer.management.domain.model.CustomerBankAccount;
import com.sixpay.customer.management.domain.model.CustomerBankAccountId;
import com.sixpay.customer.management.domain.model.CustomerId;
import com.sixpay.customer.management.domain.repository.CustomerRepository;
import com.sixpay.customer.verification.application.port.input.VerifyCustomerResult;
import com.sixpay.customer.verification.application.port.input.VerifyCustomerUseCase;
import com.sixpay.customer.verification.domain.model.AccountBindingFingerprint;
import com.sixpay.customer.verification.domain.model.VerificationOutcome;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CustomerManagementServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-08-22T20:00:00Z");

    @Test
    void updatesProfileAndPersistsAggregate() {
        Fixture fixture = fixture();

        Customer updated = fixture.service.updateProfile(
                fixture.customer.id(),
                "Updated Name",
                "updated@example.com",
                "+237699999999",
                NOW.plusSeconds(1)
        );

        assertThat(updated.legalName())
                .isEqualTo("Updated Name");
        verify(fixture.repository).save(fixture.customer);
    }

    @Test
    void lookupThenFreshVerificationThenAddsAccount() {
        Fixture fixture = fixture();

        when(fixture.ids.nextId())
                .thenReturn(
                        UUID.randomUUID(),
                        UUID.randomUUID()
                );

        when(fixture.lookup.lookup(any()))
                .thenReturn(profile(
                        fixture.customer,
                        "ACC-002",
                        "v1:" + "b".repeat(64)
                ));

        VerifyCustomerResult verified =
                mock(VerifyCustomerResult.class);

        when(verified.outcome())
                .thenReturn(VerificationOutcome.VERIFIED);
        when(verified.accountBindingFingerprint())
                .thenReturn(
                        AccountBindingFingerprint.of(
                                "v1:" + "b".repeat(64)
                        )
                );
        when(verified.observedAt()).thenReturn(NOW);
        when(verified.completedAt()).thenReturn(NOW);
        when(verified.validUntil())
                .thenReturn(NOW.plusSeconds(60));
        when(fixture.verification.verify(any()))
                .thenReturn(verified);

        Customer updated =
                fixture.service.addBankAccount(
                        fixture.customer.id(),
                        new AddBankAccountCommand(
                                "ACC-002",
                                UUID.randomUUID().toString()
                        ),
                        NOW
                );

        assertThat(updated.bankAccounts())
                .hasSize(2);

        var order = inOrder(
                fixture.lookup,
                fixture.verification,
                fixture.repository
        );
        order.verify(fixture.lookup).lookup(any());
        order.verify(fixture.verification).verify(any());
        order.verify(fixture.repository).save(
                fixture.customer
        );
    }

    @Test
    void refusesAccountResolvedForAnotherBankingCustomer() {
        Fixture fixture = fixture();

        when(fixture.lookup.lookup(any()))
                .thenReturn(
                        new BankingCustomerLookupPort.BankingCustomerProfile(
                                fixture.customer
                                        .financialInstitutionCode(),
                                "OTHER-CUSTOMER",
                                fixture.customer
                                        .customerNumber()
                                        .orElse(null),
                                fixture.customer
                                        .niu()
                                        .orElseThrow(),
                                fixture.customer.legalName(),
                                null,
                                null,
                                new BankingCustomerLookupPort.BankingAccount(
                                        "ACC-002",
                                        "v1:" + "b".repeat(64),
                                        "ACC-002",
                                        "****0002",
                                        "XAF",
                                        "CURRENT",
                                        NOW
                                )
                        )
                );

        assertThatThrownBy(() ->
                fixture.service.addBankAccount(
                        fixture.customer.id(),
                        new AddBankAccountCommand(
                                "ACC-002",
                                UUID.randomUUID().toString()
                        ),
                        NOW
                )
        ).isInstanceOf(CustomerDomainException.class)
                .hasMessageContaining(
                        "does not belong to enrolled customer"
                );

        verifyNoInteractions(fixture.verification);
        verify(fixture.repository, never()).save(any());
    }

    @Test
    void refusesNonVerifiedEvidence() {
        Fixture fixture = fixture();

        when(fixture.ids.nextId())
                .thenReturn(UUID.randomUUID());

        when(fixture.lookup.lookup(any()))
                .thenReturn(profile(
                        fixture.customer,
                        "ACC-002",
                        "v1:" + "b".repeat(64)
                ));

        VerifyCustomerResult rejected =
                mock(VerifyCustomerResult.class);
        when(rejected.outcome())
                .thenReturn(VerificationOutcome.REJECTED);
        when(fixture.verification.verify(any()))
                .thenReturn(rejected);

        assertThatThrownBy(() ->
                fixture.service.addBankAccount(
                        fixture.customer.id(),
                        new AddBankAccountCommand(
                                "ACC-002",
                                UUID.randomUUID().toString()
                        ),
                        NOW
                )
        ).isInstanceOf(CustomerDomainException.class)
                .hasMessageContaining(
                        "requires VERIFIED banking evidence"
                );

        verify(fixture.repository, never()).save(any());
    }

    private static BankingCustomerLookupPort.BankingCustomerProfile profile(
            Customer customer,
            String accountReference,
            String fingerprint
    ) {
        return new BankingCustomerLookupPort.BankingCustomerProfile(
                customer.financialInstitutionCode(),
                customer.bankingCustomerReference(),
                customer.customerNumber().orElse(null),
                customer.niu().orElseThrow(),
                customer.legalName(),
                customer.email().orElse(null),
                customer.phoneNumber().orElse(null),
                new BankingCustomerLookupPort.BankingAccount(
                        accountReference,
                        fingerprint,
                        accountReference,
                        "****0002",
                        "XAF",
                        "CURRENT",
                        NOW
                )
        );
    }

    private static Fixture fixture() {
        CustomerRepository repository =
                mock(CustomerRepository.class);
        CustomerEnrollmentIdGenerator ids =
                mock(CustomerEnrollmentIdGenerator.class);
        BankingCustomerLookupPort lookup =
                mock(BankingCustomerLookupPort.class);
        VerifyCustomerUseCase verification =
                mock(VerifyCustomerUseCase.class);

        Customer customer = customer();

        when(repository.findById(customer.id()))
                .thenReturn(Optional.of(customer));
        when(repository.save(any()))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        CustomerManagementService service =
                new CustomerManagementService(
                        repository,
                        ids,
                        lookup,
                        verification
                );

        return new Fixture(
                service,
                repository,
                ids,
                lookup,
                verification,
                customer
        );
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

    private record Fixture(
            CustomerManagementService service,
            CustomerRepository repository,
            CustomerEnrollmentIdGenerator ids,
            BankingCustomerLookupPort lookup,
            VerifyCustomerUseCase verification,
            Customer customer
    ) {
    }
}
