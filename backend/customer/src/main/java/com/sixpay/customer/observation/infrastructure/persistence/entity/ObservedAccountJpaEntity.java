package com.sixpay.customer.observation.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

@Entity
@Table(
        name = "customer_observed_account",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_customer_observed_account",
                columnNames = {"observed_institution_id", "account_binding_fingerprint"}
        )
)
public class ObservedAccountJpaEntity {

    @Id
    @GeneratedValue
    @Column(name = "observed_account_id", nullable = false, updatable = false)
    private UUID observedAccountId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "observed_institution_id", nullable = false)
    private ObservedCustomerInstitutionJpaEntity institution;

    @Column(name = "account_binding_fingerprint", nullable = false, length = 67)
    private String accountBindingFingerprint;

    @Column(name = "masked_value", nullable = false, length = 128)
    private String maskedValue;

    protected ObservedAccountJpaEntity() {
        // Required by JPA.
    }

    public static ObservedAccountJpaEntity create() {
        return new ObservedAccountJpaEntity();
    }

    public void attachTo(ObservedCustomerInstitutionJpaEntity value) {
        this.institution = value;
    }

    public void detach() {
        this.institution = null;
    }

    public UUID getObservedAccountId() { return observedAccountId; }
    public ObservedCustomerInstitutionJpaEntity getInstitution() { return institution; }
    public String getAccountBindingFingerprint() { return accountBindingFingerprint; }
    public void setAccountBindingFingerprint(String value) { this.accountBindingFingerprint = value; }
    public String getMaskedValue() { return maskedValue; }
    public void setMaskedValue(String value) { this.maskedValue = value; }
}
