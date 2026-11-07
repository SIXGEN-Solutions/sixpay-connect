package com.sixpay.customer.management.application.service;

import com.sixpay.customer.management.application.port.input.AddBankAccountCommand;
import com.sixpay.customer.management.application.port.output.BankingCustomerLookupPort;
import com.sixpay.customer.management.application.port.output.CustomerEnrollmentIdGenerator;
import com.sixpay.customer.management.domain.exception.CustomerDomainException;
import com.sixpay.customer.management.domain.model.Customer;
import com.sixpay.customer.management.domain.model.CustomerId;
import com.sixpay.customer.management.domain.repository.CustomerRepository;
import com.sixpay.customer.verification.application.port.input.VerifyCustomerUseCase;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CustomerLookupOptionalBindingTest {
    @Test
    void previewAccountCanOmitBindingHints() {
        var account = new BankingCustomerLookupPort.BankingAccount(
                "A-1", null, null, "****0001", "XAF", "CURRENT",
                Instant.parse("2026-10-01T12:00:00Z"));
        assertThat(account.accountReference()).isEqualTo("A-1");
        assertThat(account.accountBindingFingerprint()).isNull();
        assertThat(account.bankingAccountAccessReference()).isNull();
    }

    @Test
    void linkingRejectsMissingBindingBeforeVerificationAndPersistence() {
        CustomerRepository repository = mock(CustomerRepository.class);
        CustomerEnrollmentIdGenerator ids = mock(CustomerEnrollmentIdGenerator.class);
        VerifyCustomerUseCase verification = mock(VerifyCustomerUseCase.class);
        BankingCustomerLookupPort lookup = mock(BankingCustomerLookupPort.class);
        Customer customer = mock(Customer.class);
        CustomerId id = new CustomerId(UUID.randomUUID());
        when(repository.findById(id)).thenReturn(Optional.of(customer));
        when(customer.financialInstitutionCode()).thenReturn("LRB");
        when(customer.bankingCustomerReference()).thenReturn("C-1");
        when(customer.niu()).thenReturn(Optional.of("N-1"));
        when(customer.customerNumber()).thenReturn(Optional.of("123"));
        when(lookup.lookup(any())).thenReturn(new BankingCustomerLookupPort.BankingCustomerProfile(
                "LRB", "C-1", "123", "N-1", "Customer One", "c@example.com", "+237600000000",
                new BankingCustomerLookupPort.BankingAccount("A-1", null, null, "****0001", "XAF", "CURRENT",
                        Instant.parse("2026-10-01T12:00:00Z"))));
        var service = new CustomerManagementService(repository, ids, lookup, verification);
        assertThatThrownBy(() -> service.addBankAccount(
                id, new AddBankAccountCommand("A-1", UUID.randomUUID().toString()),
                Instant.parse("2026-10-01T12:00:00Z")))
                .isInstanceOf(CustomerDomainException.class)
                .hasMessage("bank account linking requires banking binding evidence");
        verifyNoInteractions(ids, verification);
        verify(repository, never()).save(any());
    }
}
