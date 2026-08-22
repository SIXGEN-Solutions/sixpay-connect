package com.sixpay.customer.management.infrastructure.persistence;

import com.sixpay.customer.management.domain.model.CustomerBankAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customer_management_bank_account")
public class CustomerBankAccountJpaEntity {

    @Id
    @Column(name = "bank_account_id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, updatable = false)
    private CustomerJpaEntity customer;

    @Column(name = "banking_account_reference", nullable = false, length = 100)
    private String bankingAccountReference;

    @Column(name = "account_binding_fingerprint", nullable = false, length = 128)
    private String accountBindingFingerprint;

    @Column(name = "masked_account_identifier", nullable = false, length = 100)
    private String maskedAccountIdentifier;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "account_type", length = 40)
    private String accountType;

    @Column(name = "default_account", nullable = false)
    private boolean defaultAccount;

    @Column(name = "verified_at", nullable = false)
    private Instant verifiedAt;

    protected CustomerBankAccountJpaEntity() {
    }

    static CustomerBankAccountJpaEntity create(
            CustomerJpaEntity customer,
            CustomerBankAccount account
    ) {
        CustomerBankAccountJpaEntity entity =
                new CustomerBankAccountJpaEntity();
        entity.id = account.id().value();
        entity.customer = customer;
        entity.synchronize(account);
        return entity;
    }

    void synchronize(CustomerBankAccount account) {
        bankingAccountReference = account.bankingAccountReference();
        accountBindingFingerprint = account.accountBindingFingerprint();
        maskedAccountIdentifier = account.maskedAccountIdentifier();
        currency = account.currency();
        accountType = account.accountType();
        defaultAccount = account.defaultAccount();
        verifiedAt = account.verifiedAt();
    }

    UUID id() {
        return id;
    }

    UUID customerId() {
        return customer.id();
    }

    String bankingAccountReference() {
        return bankingAccountReference;
    }

    String accountBindingFingerprint() {
        return accountBindingFingerprint;
    }

    String maskedAccountIdentifier() {
        return maskedAccountIdentifier;
    }

    String currency() {
        return currency;
    }

    String accountType() {
        return accountType;
    }

    boolean defaultAccount() {
        return defaultAccount;
    }

    Instant verifiedAt() {
        return verifiedAt;
    }

    void clearDefaultAccount() {
        defaultAccount = false;
    }
}
