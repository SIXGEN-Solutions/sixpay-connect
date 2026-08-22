package com.sixpay.customer.management.domain.model;

import com.sixpay.customer.management.domain.exception.CustomerDomainException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerTest {

    private static final Instant NOW =
            Instant.parse("2026-08-22T20:00:00Z");

    @Test
    void createsActiveCustomerWithOneDefaultAccount() {
        CustomerId customerId = new CustomerId(UUID.randomUUID());

        Customer customer = Customer.create(
                customerId,
                "sixpay_bank",
                "BANK-CUSTOMER-001",
                "000123",
                "NIU-001",
                "Customer One",
                "Customer.One@Example.com",
                "+237600000001",
                account(customerId, "ACCOUNT-001", "FP-001"),
                NOW
        );

        assertThat(customer.status()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(customer.acceptsNewSubscriptions()).isTrue();
        assertThat(customer.financialInstitutionCode())
                .isEqualTo("SIXPAY_BANK");
        assertThat(customer.email())
                .contains("customer.one@example.com");
        assertThat(customer.defaultBankAccount()).isPresent();
        assertThat(customer.bankAccounts()).hasSize(1);
    }

    @Test
    void rejectsAccountOwnedByAnotherCustomer() {
        CustomerId customerId = new CustomerId(UUID.randomUUID());

        assertThatThrownBy(() -> Customer.create(
                customerId,
                "SIXPAY_BANK",
                "BANK-CUSTOMER-001",
                null,
                null,
                "Customer One",
                null,
                null,
                account(
                        new CustomerId(UUID.randomUUID()),
                        "ACCOUNT-001",
                        "FP-001"
                ),
                NOW
        )).isInstanceOf(CustomerDomainException.class)
                .hasMessageContaining("another customer");
    }

    @Test
    void rejectsDuplicateAccountReferenceOrFingerprint() {
        CustomerId customerId = new CustomerId(UUID.randomUUID());
        Customer customer = customer(customerId);

        assertThatThrownBy(() -> customer.addBankAccount(
                account(customerId, "ACCOUNT-001", "FP-002"),
                NOW.plusSeconds(1)
        )).isInstanceOf(CustomerDomainException.class);

        assertThatThrownBy(() -> customer.addBankAccount(
                account(customerId, "ACCOUNT-002", "FP-001"),
                NOW.plusSeconds(1)
        )).isInstanceOf(CustomerDomainException.class);
    }

    @Test
    void managesSingleDefaultAccount() {
        CustomerId customerId = new CustomerId(UUID.randomUUID());
        Customer customer = customer(customerId);
        CustomerBankAccount second =
                account(customerId, "ACCOUNT-002", "FP-002");

        customer.addBankAccount(second, NOW.plusSeconds(1));

        assertThat(customer.defaultBankAccount())
                .get()
                .extracting(CustomerBankAccount::bankingAccountReference)
                .isEqualTo("ACCOUNT-001");

        customer.makeDefaultBankAccount(
                second.id(),
                NOW.plusSeconds(2)
        );

        assertThat(customer.defaultBankAccount())
                .get()
                .extracting(CustomerBankAccount::bankingAccountReference)
                .isEqualTo("ACCOUNT-002");
    }

    @Test
    void cannotRemoveLastBankAccount() {
        Customer customer = customer(
                new CustomerId(UUID.randomUUID())
        );

        assertThatThrownBy(() -> customer.removeBankAccount(
                customer.defaultBankAccount().orElseThrow().id(),
                NOW.plusSeconds(1)
        )).isInstanceOf(CustomerDomainException.class)
                .hasMessageContaining("at least one bank account");
    }

    @Test
    void enforcesStatusLifecycleAndClosedIsTerminal() {
        Customer customer = customer(
                new CustomerId(UUID.randomUUID())
        );

        customer.suspend("manual review", NOW.plusSeconds(1));
        assertThat(customer.status())
                .isEqualTo(CustomerStatus.SUSPENDED);
        assertThat(customer.acceptsNewSubscriptions()).isFalse();

        customer.reactivate(NOW.plusSeconds(2));
        assertThat(customer.status())
                .isEqualTo(CustomerStatus.ACTIVE);

        customer.close(
                "customer requested closure",
                NOW.plusSeconds(3)
        );
        assertThat(customer.status())
                .isEqualTo(CustomerStatus.CLOSED);

        assertThatThrownBy(() ->
                customer.reactivate(NOW.plusSeconds(4))
        ).isInstanceOf(CustomerDomainException.class);
    }

    @Test
    void reconstitutionRejectsSuspendedWithoutReason() {
        CustomerId customerId = new CustomerId(UUID.randomUUID());

        assertThatThrownBy(() -> Customer.reconstitute(
                customerId,
                "SIXPAY_BANK",
                "BANK-CUSTOMER-001",
                null,
                null,
                "Customer One",
                null,
                null,
                CustomerStatus.SUSPENDED,
                null,
                NOW,
                NOW,
                List.of(
                        CustomerBankAccount.reconstitute(
                                new CustomerBankAccountId(UUID.randomUUID()),
                                customerId,
                                "ACCOUNT-001",
                                "FP-001",
                                "****0001",
                                "XAF",
                                "CURRENT",
                                true,
                                NOW
                        )
                )
        )).isInstanceOf(CustomerDomainException.class)
                .hasMessageContaining("status reason");
    }

    private static Customer customer(CustomerId customerId) {
        return Customer.create(
                customerId,
                "SIXPAY_BANK",
                "BANK-CUSTOMER-001",
                "000123",
                "NIU-001",
                "Customer One",
                "customer@example.com",
                "+237600000001",
                account(customerId, "ACCOUNT-001", "FP-001"),
                NOW
        );
    }

    private static CustomerBankAccount account(
            CustomerId customerId,
            String reference,
            String fingerprint
    ) {
        return CustomerBankAccount.create(
                new CustomerBankAccountId(UUID.randomUUID()),
                customerId,
                reference,
                fingerprint,
                "****0001",
                "XAF",
                "CURRENT",
                NOW
        );
    }
}
