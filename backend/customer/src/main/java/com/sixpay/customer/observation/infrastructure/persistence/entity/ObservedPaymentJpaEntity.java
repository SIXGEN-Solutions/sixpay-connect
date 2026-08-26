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
    @JoinColumn(name = "observed_customer_id", nullable = false)
    private ObservedCustomerJpaEntity observedCustomer;

    @Column(name = "public_payment_reference", nullable = false, length = 128)
    private String publicPaymentReference;

    @Column(name = "financial_institution_code", nullable = false, length = 32)
    private String financialInstitutionCode;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "payment_status", nullable = false, length = 64)
    private String paymentStatus;

    @Column(name = "failure_reason_code", length = 64)
    private String failureReasonCode;

    @Column(name = "payment_created_at", nullable = false)
    private Instant paymentCreatedAt;

    @Column(name = "payment_updated_at", nullable = false)
    private Instant paymentUpdatedAt;

    protected ObservedPaymentJpaEntity() {
        // Required by JPA.
    }

    public static ObservedPaymentJpaEntity create() {
        return new ObservedPaymentJpaEntity();
    }

    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID value) { this.paymentId = value; }
    public ObservedCustomerJpaEntity getObservedCustomer() { return observedCustomer; }
    public void setObservedCustomer(ObservedCustomerJpaEntity value) { this.observedCustomer = value; }
    public String getPublicPaymentReference() { return publicPaymentReference; }
    public void setPublicPaymentReference(String value) { this.publicPaymentReference = value; }
    public String getFinancialInstitutionCode() { return financialInstitutionCode; }
    public void setFinancialInstitutionCode(String value) { this.financialInstitutionCode = value; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal value) { this.amount = value; }
    public String getCurrency() { return currency; }
    public void setCurrency(String value) { this.currency = value; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String value) { this.paymentStatus = value; }
    public String getFailureReasonCode() { return failureReasonCode; }
    public void setFailureReasonCode(String value) { this.failureReasonCode = value; }
    public Instant getPaymentCreatedAt() { return paymentCreatedAt; }
    public void setPaymentCreatedAt(Instant value) { this.paymentCreatedAt = value; }
    public Instant getPaymentUpdatedAt() { return paymentUpdatedAt; }
    public void setPaymentUpdatedAt(Instant value) { this.paymentUpdatedAt = value; }
}
