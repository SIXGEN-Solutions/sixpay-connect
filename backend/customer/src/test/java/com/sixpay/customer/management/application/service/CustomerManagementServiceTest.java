package com.sixpay.customer.management.application.service;

import com.sixpay.customer.management.application.port.input.AddBankAccountCommand;
import com.sixpay.customer.management.application.port.output.CustomerEnrollmentIdGenerator;
import com.sixpay.customer.management.domain.model.Customer;
import com.sixpay.customer.management.domain.model.CustomerBankAccount;
import com.sixpay.customer.management.domain.model.CustomerBankAccountId;
import com.sixpay.customer.management.domain.model.CustomerId;
import com.sixpay.customer.management.domain.repository.CustomerRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CustomerManagementServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-08-22T20:00:00Z");

    @Test
    void updatesProfileAndPersistsAggregate() {
        CustomerRepository repository = mock(CustomerRepository.class);
        CustomerEnrollmentIdGenerator ids =
                mock(CustomerEnrollmentIdGenerator.class);
        Customer customer = customer();

        when(repository.findById(customer.id()))
                .thenReturn(Optional.of(customer));
        when(repository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CustomerManagementService service =
                new CustomerManagementService(repository, ids);

        Customer updated = service.updateProfile(
                customer.id(),
                "Updated Name",
                "updated@example.com",
                "+237699999999",
                NOW.plusSeconds(1)
        );

        assertThat(updated.legalName())
                .isEqualTo("Updated Name");
        verify(repository).save(customer);
    }

    @Test
    void addsAccountThroughAggregate() {
        CustomerRepository repository = mock(CustomerRepository.class);
        CustomerEnrollmentIdGenerator ids =
                mock(CustomerEnrollmentIdGenerator.class);
        Customer customer = customer();

        when(repository.findById(customer.id()))
                .thenReturn(Optional.of(customer));
        when(ids.nextId()).thenReturn(UUID.randomUUID());
        when(repository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CustomerManagementService service =
                new CustomerManagementService(repository, ids);

        Customer updated = service.addBankAccount(
                customer.id(),
                new AddBankAccountCommand(
                        "ACC-002",
                        "v1:" + "b".repeat(64),
                        "****0002",
                        "XAF",
                        "SAVINGS",
                        NOW
                ),
                NOW.plusSeconds(1)
        );

        assertThat(updated.bankAccounts()).hasSize(2);
        verify(repository).save(customer);
    }

    private static Customer customer() {
        CustomerId id = new CustomerId(UUID.randomUUID());
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
                        new CustomerBankAccountId(UUID.randomUUID()),
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
