package com.sixpay.customer.management.application.service;

import com.sixpay.customer.management.application.port.input.EnrollCustomerCommand;
import com.sixpay.customer.management.application.port.output.BankingCustomerLookupPort;
import com.sixpay.customer.management.application.port.output.CustomerEnrollmentIdGenerator;
import com.sixpay.customer.management.application.port.output.CustomerEnrollmentTimeProvider;
import com.sixpay.customer.management.domain.exception.CustomerDomainException;
import com.sixpay.customer.management.domain.repository.CustomerRepository;
import com.sixpay.customer.verification.application.port.input.VerifyCustomerResult;
import com.sixpay.customer.verification.application.port.input.VerifyCustomerUseCase;
import com.sixpay.customer.verification.application.port.output.VerifiedBankingAccount;
import com.sixpay.customer.verification.application.port.output.VerifiedBankingIdentity;
import com.sixpay.customer.verification.domain.model.VerificationOutcome;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;
import com.sixpay.customer.verification.application.port.input.VerifyCustomerCommand;

class CustomerEnrollmentServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-22T20:00:00Z");

    private final VerifyCustomerUseCase verification = mock(VerifyCustomerUseCase.class);
    private final BankingCustomerLookupPort lookup = mock(BankingCustomerLookupPort.class);
    private final CustomerEnrollmentIdGenerator ids = mock(CustomerEnrollmentIdGenerator.class);
    private final CustomerEnrollmentTimeProvider time = mock(CustomerEnrollmentTimeProvider.class);
    private final CustomerRepository repository = mock(CustomerRepository.class);

    private CustomerEnrollmentService service() {
        when(time.now()).thenReturn(NOW);
        when(ids.nextId()).thenReturn(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        when(lookup.lookup(any())).thenReturn(consulted("ACC-001", "XAF"));
        return new CustomerEnrollmentService(verification, lookup, ids, time, repository);
    }

    private EnrollCustomerCommand command() {
        return new EnrollCustomerCommand("SIXPAY_BANK", "NIU-001", null,
                "ACC-001", UUID.randomUUID().toString());
    }

    private BankingCustomerLookupPort.BankingCustomerProfile consulted(
            String accountReference, String currency) {
        return new BankingCustomerLookupPort.BankingCustomerProfile(
                "SIXPAY_BANK", "CUST-001", "000123", "NIU-001",
                "Customer One", "customer@example.com", "+237600000001",
                new BankingCustomerLookupPort.BankingAccount(
                        accountReference, null, null, "****4321", currency,
                        "SAVINGS", NOW.plusSeconds(1)));
    }

    private VerifyCustomerResult verified(String accountReference) {
        VerifyCustomerResult result = mock(VerifyCustomerResult.class);
        when(result.outcome()).thenReturn(VerificationOutcome.VERIFIED);
        when(result.observedAt()).thenReturn(NOW);
        when(result.completedAt()).thenReturn(NOW);
        when(result.validUntil()).thenReturn(NOW.plusSeconds(60));
        when(result.customerReference()).thenReturn("CUST-001");
        when(result.accountReference()).thenReturn(accountReference);
        when(result.identityOptional()).thenReturn(java.util.Optional.of(
                new VerifiedBankingIdentity("CUST-001", "000123", "SIXPAY_BANK",
                        "NIU-001", "Customer One", "+237600000001", "customer@example.com",
                        "VERIFIED", List.of(), NOW, NOW)));
        when(result.accountOptional()).thenReturn(java.util.Optional.of(
                new VerifiedBankingAccount(accountReference, "CUST-001", "SIXPAY_BANK",
                        "****0001", "XAF", "CURRENT", "ACTIVE", List.of(), NOW)));
        return result;
    }

    @Test
    void verifiesOnceThenCreatesCustomerFromBankingEvidence() {
        var service = service();
        VerifyCustomerResult verifiedResult = verified("ACC-001");
        when(verification.verify(any())).thenReturn(verifiedResult);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var result = service.enroll(command());
        assertThat(result.customer().legalName()).isEqualTo("Customer One");
        assertThat(result.customer().defaultBankAccount()).isPresent();
        assertThat(result.customer().defaultBankAccount().orElseThrow()
                .accountBindingFingerprint()).matches("v1:[0-9a-f]{64}");
        ArgumentCaptor<VerifyCustomerCommand> captured =
                ArgumentCaptor.forClass(VerifyCustomerCommand.class);
        verify(verification, times(1)).verify(captured.capture());
        assertThat(captured.getValue().bankingAccountAccessReference().value())
                .isEqualTo("ACC-001");
        assertThat(result.customer().defaultBankAccount().orElseThrow()
                .maskedAccountIdentifier()).isEqualTo("****4321");
        assertThat(result.customer().defaultBankAccount().orElseThrow()
                .accountType()).isEqualTo("SAVINGS");
        verify(lookup).lookup(any());
        verify(repository).save(any());
    }

    @Test
    void mismatchedConsultedAccountDoesNotPersist() {
        var service = service();
        VerifyCustomerResult verifiedResult = verified("ACC-001");

        when(verification.verify(any()))
                .thenReturn(verifiedResult);
        when(lookup.lookup(any())).thenReturn(consulted("OTHER-ACCOUNT", "XAF"));
        assertThatThrownBy(() -> service.enroll(command()))
                .isInstanceOf(CustomerDomainException.class)
                .hasMessageContaining("verified and consulted");
        verify(repository, never()).save(any());
    }

    @Test
    void bankNativeCurrencyWithoutApprovedMappingCannotPersist() {
        var service = service();
        VerifyCustomerResult verifiedResult = verified("ACC-001");

        when(verification.verify(any()))
                .thenReturn(verifiedResult);
        when(lookup.lookup(any())).thenReturn(consulted("ACC-001", "001"));
        assertThatThrownBy(() -> service.enroll(command()))
                .isInstanceOf(CustomerDomainException.class)
                .hasMessageContaining("ISO-4217");
        verify(repository, never()).save(any());
    }

    @Test
    void negativeVerificationDoesNotPersist() {
        var service = service();
        VerifyCustomerResult result = mock(VerifyCustomerResult.class);
        when(result.outcome()).thenReturn(VerificationOutcome.REJECTED);
        when(verification.verify(any())).thenReturn(result);
        assertThatThrownBy(() -> service.enroll(command()))
                .isInstanceOf(CustomerDomainException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void mismatchedBankAccountDoesNotPersist() {
        var service = service();
        VerifyCustomerResult mismatchedResult = verified("ANOTHER-ACCOUNT");
        when(verification.verify(any())).thenReturn(mismatchedResult);
        assertThatThrownBy(() -> service.enroll(command()))
                .isInstanceOf(CustomerDomainException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void unavailableVerificationDoesNotPersist() {
        var service = service();
        when(verification.verify(any())).thenThrow(new IllegalStateException("bank unavailable"));
        assertThatThrownBy(() -> service.enroll(command()))
                .isInstanceOf(IllegalStateException.class);
        verify(repository, never()).save(any());
    }
}
