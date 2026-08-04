package com.sixpay.customer.observation.infrastructure.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "customer_observed_customer")
public class ObservedCustomerJpaEntity {

    @Id
    @Column(name = "observed_customer_id", nullable = false, updatable = false)
    private UUID observedCustomerId;

    @Column(name = "niu_protected", nullable = false, length = 1024)
    private String niuProtected;

    @Column(name = "niu_search_hash", nullable = false, unique = true, length = 64)
    private String niuSearchHash;

    @Column(name = "legal_name_protected", nullable = false, length = 2048)
    private String legalNameProtected;

    @Column(name = "legal_name_search_normalized", nullable = false, length = 256)
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

    @Column(name = "last_payment_status", nullable = false, length = 64)
    private String lastPaymentStatus;

    @Column(name = "last_failure_reason_code", length = 64)
    private String lastFailureReasonCode;

    @Column(name = "projection_version", nullable = false)
    private long projectionVersion;

    @Column(name = "source_event_watermark", nullable = false, length = 256)
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
    private Set<ObservedCustomerInstitutionJpaEntity> institutions =
            new LinkedHashSet<>();

    protected ObservedCustomerJpaEntity() {
        // Required by JPA.
    }

    public static ObservedCustomerJpaEntity create() {
        return new ObservedCustomerJpaEntity();
    }

    public void addInstitution(
            ObservedCustomerInstitutionJpaEntity institution
    ) {
        institution.attachTo(this);
        institutions.add(institution);
    }

    public void removeInstitution(
            ObservedCustomerInstitutionJpaEntity institution
    ) {
        institutions.remove(institution);
        institution.detach();
    }

    public Set<ObservedCustomerInstitutionJpaEntity> mutableInstitutions() {
        return institutions;
    }

    public Set<ObservedCustomerInstitutionJpaEntity> getInstitutions() {
        return Set.copyOf(institutions);
    }

    public UUID getObservedCustomerId() { return observedCustomerId; }
    public void setObservedCustomerId(UUID value) { this.observedCustomerId = value; }
    public String getNiuProtected() { return niuProtected; }
    public void setNiuProtected(String value) { this.niuProtected = value; }
    public String getNiuSearchHash() { return niuSearchHash; }
    public void setNiuSearchHash(String value) { this.niuSearchHash = value; }
    public String getLegalNameProtected() { return legalNameProtected; }
    public void setLegalNameProtected(String value) { this.legalNameProtected = value; }
    public String getLegalNameSearchNormalized() { return legalNameSearchNormalized; }
    public void setLegalNameSearchNormalized(String value) { this.legalNameSearchNormalized = value; }
    public String getPhoneMasked() { return phoneMasked; }
    public void setPhoneMasked(String value) { this.phoneMasked = value; }
    public String getEmailMasked() { return emailMasked; }
    public void setEmailMasked(String value) { this.emailMasked = value; }
    public Instant getFirstObservedAt() { return firstObservedAt; }
    public void setFirstObservedAt(Instant value) { this.firstObservedAt = value; }
    public Instant getLastObservedAt() { return lastObservedAt; }
    public void setLastObservedAt(Instant value) { this.lastObservedAt = value; }
    public long getTotalPayments() { return totalPayments; }
    public void setTotalPayments(long value) { this.totalPayments = value; }
    public long getSuccessfulPayments() { return successfulPayments; }
    public void setSuccessfulPayments(long value) { this.successfulPayments = value; }
    public long getFailedPayments() { return failedPayments; }
    public void setFailedPayments(long value) { this.failedPayments = value; }
    public String getLastPaymentStatus() { return lastPaymentStatus; }
    public void setLastPaymentStatus(String value) { this.lastPaymentStatus = value; }
    public String getLastFailureReasonCode() { return lastFailureReasonCode; }
    public void setLastFailureReasonCode(String value) { this.lastFailureReasonCode = value; }
    public long getProjectionVersion() { return projectionVersion; }
    public void setProjectionVersion(long value) { this.projectionVersion = value; }
    public String getSourceEventWatermark() { return sourceEventWatermark; }
    public void setSourceEventWatermark(String value) { this.sourceEventWatermark = value; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { this.createdAt = value; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant value) { this.updatedAt = value; }
    public long getRowVersion() { return rowVersion; }
}
