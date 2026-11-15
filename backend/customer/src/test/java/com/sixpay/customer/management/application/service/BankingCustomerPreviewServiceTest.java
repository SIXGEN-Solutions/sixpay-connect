package com.sixpay.customer.management.application.service;

import com.sixpay.customer.management.application.port.input.BankingCustomerPreviewUseCase;
import com.sixpay.customer.management.application.port.output.BankingCustomerLookupPort;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BankingCustomerPreviewServiceTest {

    @Test
    void searchesByCustomerNumberAndPreservesAllAccounts() {
        var lookup = mock(BankingCustomerLookupPort.class);
        Instant now = Instant.parse("2026-11-14T10:00:00Z");
        var accounts = List.of(
                new BankingCustomerLookupPort.BankingAccount(
                        "A-1", null, null, "****0001", "001", "CURRENT", "ACTIVE", now),
                new BankingCustomerLookupPort.BankingAccount(
                        "A-2", null, null, "****0002", "001", "SAVINGS", "ACTIVE", now));
        when(lookup.lookup(any())).thenReturn(
                new BankingCustomerLookupPort.BankingCustomerProfile(
                        "REGIONAL", "C-1", "123", null,
                        "Client", null, null, null, accounts));

        var preview = new BankingCustomerPreviewService(lookup).preview(
                new BankingCustomerPreviewUseCase.BankingCustomerPreviewQuery(
                        "REGIONAL", "123", "corr-123"));

        assertThat(preview.accounts()).hasSize(2);
        assertThat(preview.accounts()).extracting(
                BankingCustomerPreviewUseCase.BankingCustomerAccountPreview::accountReference)
                .containsExactly("A-1", "A-2");
        var captor = ArgumentCaptor.forClass(BankingCustomerLookupPort.BankingCustomerLookupQuery.class);
        verify(lookup).lookup(captor.capture());
        assertThat(captor.getValue().customerNumber()).isEqualTo("123");
        assertThat(captor.getValue().accountReference()).isNull();
        assertThat(captor.getValue().niu()).isNull();
    }
}
