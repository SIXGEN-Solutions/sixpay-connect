package com.sixpay.customer.observation.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customer_observed_payment")
public class ObservedPaymentJpaEntity {

    @Id
    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "observed_customer_id",
            nullable = false
    )
    private ObservedCustomerJpaEntity observedCustomer;

    @Column(
            name = "public_payment_reference",
            nullable = false,
            length = 128
    )
    private String publicPaymentReference;

    @Column(
            name = "financial_institution_code",
            nullable = false,
            length = 32
    )
    private String financialInstitutionCode;

    @Column(
            name = "amount",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(
            name = "payment_status",
            nullable = false,
            length = 64
    )
    private String paymentStatus;

    @Column(name = "failure_reason_code", length = 64)
    private String failureReasonCode;

    @Column(name = "payment_created_at", nullable = false)
    private Instant paymentCreatedAt;

    @Column(name = "payment_updated_at", nullable = false)
    private Instant paymentUpdatedAt;

    public ObservedPaymentJpaEntity() {
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(UUID paymentId) {
        this.paymentId = paymentId;
    }

    public ObservedCustomerJpaEntity getObservedCustomer() {
        return observedCustomer;
    }

    public void setObservedCustomer(
            ObservedCustomerJpaEntity observedCustomer
    ) {
        this.observedCustomer = observedCustomer;
    }

    public String getPublicPaymentReference() {
        return publicPaymentReference;
    }

    public void setPublicPaymentReference(
            String publicPaymentReference
    ) {
        this.publicPaymentReference = publicPaymentReference;
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getFailureReasonCode() {
        return failureReasonCode;
    }

    public void setFailureReasonCode(
            String failureReasonCode
    ) {
        this.failureReasonCode = failureReasonCode;
    }

    public Instant getPaymentCreatedAt() {
        return paymentCreatedAt;
    }

    public void setPaymentCreatedAt(Instant paymentCreatedAt) {
        this.paymentCreatedAt = paymentCreatedAt;
    }

    public Instant getPaymentUpdatedAt() {
        return paymentUpdatedAt;
    }

    public void setPaymentUpdatedAt(Instant paymentUpdatedAt) {
        this.paymentUpdatedAt = paymentUpdatedAt;
    }
}
