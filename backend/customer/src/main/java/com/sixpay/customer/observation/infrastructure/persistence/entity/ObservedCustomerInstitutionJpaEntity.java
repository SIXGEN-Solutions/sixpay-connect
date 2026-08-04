package com.sixpay.customer.observation.infrastructure.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "customer_observed_institution",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_customer_observed_institution",
                columnNames = {
                        "observed_customer_id",
                        "financial_institution_code"
                }
        )
)
public class ObservedCustomerInstitutionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "observed_institution_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "observed_customer_id",
            nullable = false
    )
    private ObservedCustomerJpaEntity observedCustomer;

    @Column(
            name = "financial_institution_code",
            nullable = false,
            length = 32
    )
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
    @OrderBy("maskedValue ASC")
    private List<ObservedAccountJpaEntity> accounts =
            new ArrayList<>();

    public ObservedCustomerInstitutionJpaEntity() {
    }

    public void attachTo(
            ObservedCustomerJpaEntity observedCustomer
    ) {
        this.observedCustomer = observedCustomer;
    }

    public void replaceAccounts(
            List<ObservedAccountJpaEntity> values
    ) {
        accounts.clear();
        values.forEach(value -> {
            value.attachTo(this);
            accounts.add(value);
        });
    }

    public String getFinancialInstitutionCode() {
        return financialInstitutionCode;
    }

    public void setFinancialInstitutionCode(
            String financialInstitutionCode
    ) {
        this.financialInstitutionCode =
                financialInstitutionCode;
    }

    public Instant getFirstObservedAt() {
        return firstObservedAt;
    }

    public void setFirstObservedAt(Instant firstObservedAt) {
        this.firstObservedAt = firstObservedAt;
    }

    public Instant getLastObservedAt() {
        return lastObservedAt;
    }

    public void setLastObservedAt(Instant lastObservedAt) {
        this.lastObservedAt = lastObservedAt;
    }

    public List<ObservedAccountJpaEntity> getAccounts() {
        return accounts;
    }
}
