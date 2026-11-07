package com.sixpay.customer.management.domain.model;

import com.sixpay.customer.management.domain.exception.CustomerDomainException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerBankAccountTest {

    @Test
    void preservesNativeCurrencyAndOpaqueReferences() {
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
        assertThat(account.currency()).isEqualTo("xaf");
        assertThat(account.defaultAccount()).isFalse();
    }

    @Test
    void acceptsNativeBankDevCode() {
        CustomerBankAccount account = CustomerBankAccount.create(
                new CustomerBankAccountId(UUID.randomUUID()),
                new CustomerId(UUID.randomUUID()),
                "00001-04942231051-47",
                "fingerprint-001",
                "****1051",
                "001",
                "CURRENT",
                Instant.parse("2026-08-22T20:00:00Z")
        );
        assertThat(account.currency()).isEqualTo("001");
    }

    @Test
    void rejectsMissingCurrency() {
        assertThatThrownBy(() -> CustomerBankAccount.create(
                new CustomerBankAccountId(UUID.randomUUID()),
                new CustomerId(UUID.randomUUID()),
                "BANK-ACCOUNT-001",
                "fingerprint-001",
                "****001",
                " ",
                null,
                Instant.parse("2026-08-22T20:00:00Z")
        )).isInstanceOf(CustomerDomainException.class);
    }
}
