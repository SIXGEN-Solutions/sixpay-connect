package com.sixpay.customer.management.infrastructure.persistence;

import com.sixpay.customer.management.domain.model.Customer;
import com.sixpay.customer.management.domain.model.CustomerBankAccountId;
import com.sixpay.customer.management.domain.model.CustomerStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "customer_management_customer")
public class CustomerJpaEntity {

    @Id
    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "financial_institution_code", nullable = false, length = 32)
    private String financialInstitutionCode;

    @Column(name = "banking_customer_reference", nullable = false, length = 100)
    private String bankingCustomerReference;

    @Column(name = "customer_number", length = 100)
    private String customerNumber;

    @Column(name = "niu", length = 100)
    private String niu;

    @Column(name = "legal_name", nullable = false, length = 200)
    private String legalName;

    @Column(name = "email", length = 254)
    private String email;

    @Column(name = "phone_number", length = 32)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private CustomerStatus status;

    @Column(name = "status_reason", length = 500)
    private String statusReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private long version;

    @OneToMany(
            mappedBy = "customer",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<CustomerBankAccountJpaEntity> bankAccounts =
            new ArrayList<>();

    protected CustomerJpaEntity() {
    }

    static CustomerJpaEntity create(Customer customer) {
        CustomerJpaEntity entity = new CustomerJpaEntity();
        entity.id = customer.id().value();
        entity.createdAt = customer.createdAt();
        entity.synchronize(customer);
        return entity;
    }

    void synchronize(Customer customer) {
        financialInstitutionCode = customer.financialInstitutionCode();
        bankingCustomerReference = customer.bankingCustomerReference();
        customerNumber = customer.customerNumber().orElse(null);
        niu = customer.niu().orElse(null);
        legalName = customer.legalName();
        email = customer.email().orElse(null);
        phoneNumber = customer.phoneNumber().orElse(null);
        status = customer.status();
        statusReason = customer.statusReason().orElse(null);
        updatedAt = customer.updatedAt();

        Map<UUID, CustomerBankAccountJpaEntity> existing =
                new HashMap<>();
        for (CustomerBankAccountJpaEntity account : bankAccounts) {
            existing.put(account.id(), account);
        }

        var targetIds = customer.bankAccounts()
                .stream()
                .map(account -> account.id().value())
                .collect(java.util.stream.Collectors.toSet());

        bankAccounts.removeIf(
                account -> !targetIds.contains(account.id())
        );

        for (var account : customer.bankAccounts()) {
            CustomerBankAccountJpaEntity entity =
                    existing.get(account.id().value());
            if (entity == null) {
                entity = CustomerBankAccountJpaEntity.create(
                        this,
                        account
                );
                bankAccounts.add(entity);
            } else {
                entity.synchronize(account);
            }
        }
    }

    UUID id() {
        return id;
    }

    String financialInstitutionCode() {
        return financialInstitutionCode;
    }

    String bankingCustomerReference() {
        return bankingCustomerReference;
    }

    String customerNumber() {
        return customerNumber;
    }

    String niu() {
        return niu;
    }

    String legalName() {
        return legalName;
    }

    String email() {
        return email;
    }

    String phoneNumber() {
        return phoneNumber;
    }

    CustomerStatus status() {
        return status;
    }

    String statusReason() {
        return statusReason;
    }

    Instant createdAt() {
        return createdAt;
    }

    Instant updatedAt() {
        return updatedAt;
    }

    List<CustomerBankAccountJpaEntity> bankAccounts() {
        return List.copyOf(bankAccounts);
    }

    boolean prepareDefaultAccountSwitch(
            Customer customer
    ) {
        UUID targetDefaultAccountId =
                customer.defaultBankAccount()
                        .orElseThrow()
                        .id()
                        .value();

        boolean changed = false;

        for (CustomerBankAccountJpaEntity account
                : bankAccounts) {

            if (account.defaultAccount()
                    && !account.id()
                    .equals(targetDefaultAccountId)) {

                account.clearDefaultAccount();
                changed = true;
            }
        }

        return changed;
    }
}
