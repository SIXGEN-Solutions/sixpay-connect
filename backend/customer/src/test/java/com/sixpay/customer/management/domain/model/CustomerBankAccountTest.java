package com.sixpay.customer.management.domain.model;

import com.sixpay.customer.management.domain.exception.CustomerDomainException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerBankAccountTest {

    @Test
    void normalizesCurrencyAndOpaqueReferences() {
        CustomerBankAccount account = CustomerBankAccount.create(
                new CustomerBankAccountId(UUID.randomUUID()),
                new CustomerId(UUID.randomUUID()),
                "  BANK-ACCOUNT-001 ",
                "  fingerprint-001 ",
                " ****001 ",
                "xaf",
                "CURRENT",
                Instant.parse("2026-08-22T20:00:00Z")
        );

        assertThat(account.bankingAccountReference())
                .isEqualTo("BANK-ACCOUNT-001");
        assertThat(account.currency()).isEqualTo("XAF");
        assertThat(account.defaultAccount()).isFalse();
    }

    @Test
    void rejectsInvalidCurrency() {
        assertThatThrownBy(() -> CustomerBankAccount.create(
                new CustomerBankAccountId(UUID.randomUUID()),
                new CustomerId(UUID.randomUUID()),
                "BANK-ACCOUNT-001",
                "fingerprint-001",
                "****001",
                "INVALID",
                null,
                Instant.parse("2026-08-22T20:00:00Z")
        )).isInstanceOf(CustomerDomainException.class);
    }
}
