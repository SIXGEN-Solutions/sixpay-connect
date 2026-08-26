package com.sixpay.customer.observation.infrastructure.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
        name = "customer_observed_institution",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_customer_observed_institution",
                columnNames = {"observed_customer_id", "financial_institution_code"}
        )
)
public class ObservedCustomerInstitutionJpaEntity {

    @Id
    @GeneratedValue
    @Column(name = "observed_institution_id", nullable = false, updatable = false)
    private UUID observedInstitutionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "observed_customer_id", nullable = false)
    private ObservedCustomerJpaEntity observedCustomer;

    @Column(name = "financial_institution_code", nullable = false, length = 32)
    private String financialInstitutionCode;

    @Column(name = "first_observed_at", nullable = false)
    private Instant firstObservedAt;

    @Column(name = "last_observed_at", nullable = false)
    private Instant lastObservedAt;

    @OneToMany(
            mappedBy = "institution",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private Set<ObservedAccountJpaEntity> accounts = new LinkedHashSet<>();

    protected ObservedCustomerInstitutionJpaEntity() {
        // Required by JPA.
    }

    public static ObservedCustomerInstitutionJpaEntity create() {
        return new ObservedCustomerInstitutionJpaEntity();
    }

    public void attachTo(ObservedCustomerJpaEntity customer) {
        this.observedCustomer = customer;
    }

    public void detach() {
        this.observedCustomer = null;
    }

    public void addAccount(ObservedAccountJpaEntity account) {
        account.attachTo(this);
        accounts.add(account);
    }

    public void removeAccount(ObservedAccountJpaEntity account) {
        accounts.remove(account);
        account.detach();
    }

    public Set<ObservedAccountJpaEntity> mutableAccounts() {
        return accounts;
    }

    public Set<ObservedAccountJpaEntity> getAccounts() {
        return Set.copyOf(accounts);
    }

    public UUID getObservedInstitutionId() { return observedInstitutionId; }
    public ObservedCustomerJpaEntity getObservedCustomer() { return observedCustomer; }
    public String getFinancialInstitutionCode() { return financialInstitutionCode; }
    public void setFinancialInstitutionCode(String value) { this.financialInstitutionCode = value; }
    public Instant getFirstObservedAt() { return firstObservedAt; }
    public void setFirstObservedAt(Instant value) { this.firstObservedAt = value; }
    public Instant getLastObservedAt() { return lastObservedAt; }
    public void setLastObservedAt(Instant value) { this.lastObservedAt = value; }
}
