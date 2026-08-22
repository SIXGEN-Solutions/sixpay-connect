package com.sixpay.customer.management.application.service;

import com.sixpay.customer.management.application.port.input.EnrollCustomerCommand;
import com.sixpay.customer.management.application.port.output.BankingCustomerLookupPort;
import com.sixpay.customer.management.application.port.output.CustomerEnrollmentIdGenerator;
import com.sixpay.customer.management.application.port.output.CustomerEnrollmentTimeProvider;
import com.sixpay.customer.verification.application.port.input.VerifyCustomerResult;
import com.sixpay.customer.verification.application.port.input.VerifyCustomerUseCase;
import com.sixpay.customer.verification.domain.model.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CustomerEnrollmentServiceTest {

    @Test
    void lookupThenFreshVerificationThenCreatesCustomer() {
        BankingCustomerLookupPort lookup = mock(BankingCustomerLookupPort.class);
        VerifyCustomerUseCase verification = mock(VerifyCustomerUseCase.class);
        CustomerEnrollmentIdGenerator ids = mock(CustomerEnrollmentIdGenerator.class);
        CustomerEnrollmentTimeProvider time = mock(CustomerEnrollmentTimeProvider.class);

        Instant now = Instant.parse("2026-08-22T20:00:00Z");
        when(time.now()).thenReturn(now);
        when(ids.nextId()).thenReturn(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        when(lookup.lookup(any())).thenReturn(
                new BankingCustomerLookupPort.BankingCustomerProfile(
                        "SIXPAY_BANK",
                        "CUST-001",
                        "000123",
                        "NIU-001",
                        "Customer One",
                        "customer@example.com",
                        "+237600000001",
                        new BankingCustomerLookupPort.BankingAccount(
                                "ACC-001",
                                "v1:" + "a".repeat(64),
                                "ACC-001",
                                "****0001",
                                "XAF",
                                "CURRENT",
                                now
                        )
                )
        );

        VerifyCustomerResult verified = mock(VerifyCustomerResult.class);
        when(verified.outcome()).thenReturn(VerificationOutcome.VERIFIED);
        when(verified.accountBindingFingerprint()).thenReturn(
                AccountBindingFingerprint.of("v1:" + "a".repeat(64))
        );
        when(verified.observedAt()).thenReturn(now);
        when(verified.completedAt()).thenReturn(now);
        when(verified.validUntil()).thenReturn(now.plusSeconds(60));
        when(verification.verify(any())).thenReturn(verified);

        CustomerEnrollmentService service =
                new CustomerEnrollmentService(
                        lookup,
                        verification,
                        ids,
                        time
                );

        var result = service.enroll(
                new EnrollCustomerCommand(
                        "SIXPAY_BANK",
                        "NIU-001",
                        null,
                        "ACC-001",
                        UUID.randomUUID().toString()
                )
        );

        assertThat(result.customer().legalName())
                .isEqualTo("Customer One");
        assertThat(result.customer().defaultBankAccount())
                .isPresent();

        var order = inOrder(lookup, verification);
        order.verify(lookup).lookup(any());
        order.verify(verification).verify(any());
    }
}
