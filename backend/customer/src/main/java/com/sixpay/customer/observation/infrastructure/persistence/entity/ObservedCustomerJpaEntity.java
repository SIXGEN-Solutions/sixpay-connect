package com.sixpay.customer.observation.infrastructure.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "customer_observed_customer")
public class ObservedCustomerJpaEntity {

    @Id
    @Column(name = "observed_customer_id", nullable = false)
    private UUID observedCustomerId;

    @Column(name = "niu_protected", nullable = false, length = 1024)
    private String niuProtected;

    @Column(
            name = "niu_search_hash",
            nullable = false,
            unique = true,
            length = 64
    )
    private String niuSearchHash;

    @Column(
            name = "legal_name_protected",
            nullable = false,
            length = 2048
    )
    private String legalNameProtected;

    @Column(
            name = "legal_name_search_normalized",
            nullable = false,
            length = 256
    )
    private String legalNameSearchNormalized;

    @Column(name = "phone_masked", length = 128)
    private String phoneMasked;

    @Column(name = "email_masked", length = 128)
    private String emailMasked;

    @Column(name = "first_observed_at", nullable = false)
    private Instant firstObservedAt;

    @Column(name = "last_observed_at", nullable = false)
    private Instant lastObservedAt;

    @Column(name = "total_payments", nullable = false)
    private long totalPayments;

    @Column(name = "successful_payments", nullable = false)
    private long successfulPayments;

    @Column(name = "failed_payments", nullable = false)
    private long failedPayments;

    @Column(
            name = "last_payment_status",
            nullable = false,
            length = 64
    )
    private String lastPaymentStatus;

    @Column(name = "last_failure_reason_code", length = 64)
    private String lastFailureReasonCode;

    @Column(name = "projection_version", nullable = false)
    private long projectionVersion;

    @Column(
            name = "source_event_watermark",
            nullable = false,
            length = 256
    )
    private String sourceEventWatermark;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    @OneToMany(
            mappedBy = "observedCustomer",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @OrderBy("financialInstitutionCode ASC")
    private List<ObservedCustomerInstitutionJpaEntity> institutions =
            new ArrayList<>();

    public ObservedCustomerJpaEntity() {
    }

    public UUID getObservedCustomerId() {
        return observedCustomerId;
    }

    public void setObservedCustomerId(UUID observedCustomerId) {
        this.observedCustomerId = observedCustomerId;
    }

    public String getNiuProtected() {
        return niuProtected;
    }

    public void setNiuProtected(String niuProtected) {
        this.niuProtected = niuProtected;
    }

    public String getNiuSearchHash() {
        return niuSearchHash;
    }

    public void setNiuSearchHash(String niuSearchHash) {
        this.niuSearchHash = niuSearchHash;
    }

    public String getLegalNameProtected() {
        return legalNameProtected;
    }

    public void setLegalNameProtected(String legalNameProtected) {
        this.legalNameProtected = legalNameProtected;
    }

    public String getLegalNameSearchNormalized() {
        return legalNameSearchNormalized;
    }

    public void setLegalNameSearchNormalized(
            String legalNameSearchNormalized
    ) {
        this.legalNameSearchNormalized =
                legalNameSearchNormalized;
    }

    public String getPhoneMasked() {
        return phoneMasked;
    }

    public void setPhoneMasked(String phoneMasked) {
        this.phoneMasked = phoneMasked;
    }

    public String getEmailMasked() {
        return emailMasked;
    }

    public void setEmailMasked(String emailMasked) {
        this.emailMasked = emailMasked;
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

    public long getTotalPayments() {
        return totalPayments;
    }

    public void setTotalPayments(long totalPayments) {
        this.totalPayments = totalPayments;
    }

    public long getSuccessfulPayments() {
        return successfulPayments;
    }

    public void setSuccessfulPayments(long successfulPayments) {
        this.successfulPayments = successfulPayments;
    }

    public long getFailedPayments() {
        return failedPayments;
    }

    public void setFailedPayments(long failedPayments) {
        this.failedPayments = failedPayments;
    }

    public String getLastPaymentStatus() {
        return lastPaymentStatus;
    }

    public void setLastPaymentStatus(String lastPaymentStatus) {
        this.lastPaymentStatus = lastPaymentStatus;
    }

    public String getLastFailureReasonCode() {
        return lastFailureReasonCode;
    }

    public void setLastFailureReasonCode(
            String lastFailureReasonCode
    ) {
        this.lastFailureReasonCode = lastFailureReasonCode;
    }

    public long getProjectionVersion() {
        return projectionVersion;
    }

    public void setProjectionVersion(long projectionVersion) {
        this.projectionVersion = projectionVersion;
    }

    public String getSourceEventWatermark() {
        return sourceEventWatermark;
    }

    public void setSourceEventWatermark(
            String sourceEventWatermark
    ) {
        this.sourceEventWatermark = sourceEventWatermark;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public long getRowVersion() {
        return rowVersion;
    }

    public List<ObservedCustomerInstitutionJpaEntity>
            getInstitutions() {
        return institutions;
    }

    public void replaceInstitutions(
            List<ObservedCustomerInstitutionJpaEntity> values
    ) {
        institutions.clear();
        values.forEach(value -> {
            value.attachTo(this);
            institutions.add(value);
        });
    }
}
