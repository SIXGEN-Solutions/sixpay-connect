package com.sixpay.customer.observation.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "customer_observed_account",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_customer_observed_account",
                columnNames = {
                        "observed_institution_id",
                        "account_binding_fingerprint"
                }
        )
)
public class ObservedAccountJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "observed_account_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "observed_institution_id",
            nullable = false
    )
    private ObservedCustomerInstitutionJpaEntity institution;

    @Column(
            name = "account_binding_fingerprint",
            nullable = false,
            length = 67
    )
    private String accountBindingFingerprint;

    @Column(name = "masked_value", nullable = false, length = 32)
    private String maskedValue;

    public ObservedAccountJpaEntity() {
    }

    public void attachTo(
            ObservedCustomerInstitutionJpaEntity institution
    ) {
        this.institution = institution;
    }

    public String getAccountBindingFingerprint() {
        return accountBindingFingerprint;
    }

    public void setAccountBindingFingerprint(
            String accountBindingFingerprint
    ) {
        this.accountBindingFingerprint =
                accountBindingFingerprint;
    }

    public String getMaskedValue() {
        return maskedValue;
    }

    public void setMaskedValue(String maskedValue) {
        this.maskedValue = maskedValue;
    }
}
