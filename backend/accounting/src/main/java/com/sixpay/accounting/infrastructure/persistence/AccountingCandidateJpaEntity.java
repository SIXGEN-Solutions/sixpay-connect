package com.sixpay.accounting.infrastructure.persistence;

import com.sixpay.accounting.domain.model.AccountingCandidateProjection;
import com.sixpay.accounting.domain.model.PartnerExternalPaymentStatusEvidence;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "accounting_payment_candidates",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_accounting_payment_candidates_event_id", columnNames = "event_id"),
                @UniqueConstraint(name = "uk_accounting_payment_candidates_business_identity", columnNames = {"payment_id","financial_snapshot_id"})
        }
)
public class AccountingCandidateJpaEntity {
    @Id @Column(name="id", nullable=false, updatable=false) private UUID id;
    @Column(name="event_id", nullable=false, updatable=false) private UUID eventId;
    @Column(name="payment_id", nullable=false, updatable=false) private UUID paymentId;
    @Column(name="public_payment_reference", nullable=false, updatable=false, length=128) private String publicPaymentReference;
    @Column(name="partner_id", nullable=false, updatable=false, length=128) private String partnerId;
    @Column(name="financial_institution_code", nullable=false, updatable=false, length=64) private String financialInstitutionCode;
    @Column(name="t0_outcome", nullable=false, updatable=false, length=32) private String t0Outcome;
    @Column(name="bank_reference", nullable=false, updatable=false, length=128) private String bankReference;
    @Column(name="t0_observed_at", nullable=false, updatable=false) private Instant t0ObservedAt;
    @Column(name="accounting_business_date", nullable=false, updatable=false) private LocalDate accountingBusinessDate;
    @Column(name="financial_snapshot_id", nullable=false, updatable=false) private UUID financialSnapshotId;
    @Column(name="financial_snapshot_version", nullable=false, updatable=false, length=32) private String financialSnapshotVersion;
    @Column(name="financial_snapshot_finalized_at", nullable=false, updatable=false) private Instant financialSnapshotFinalizedAt;
    @Column(name="debtor_account_reference", nullable=false, updatable=false, length=256) private String debtorAccountReference;
    @Column(name="creditor_account_reference", nullable=false, updatable=false, length=256) private String creditorAccountReference;
    @Column(name="amount", nullable=false, updatable=false, precision=19, scale=4) private BigDecimal amount;
    @Column(name="currency", nullable=false, updatable=false, length=3) private String currency;
    @Column(name="payment_occurred_at", nullable=false, updatable=false) private Instant paymentOccurredAt;
    @Column(name="candidate_created_at", nullable=false, updatable=false) private Instant candidateCreatedAt;
    @Column(name="batch_id") private UUID batchId;

    @Column(name="partner_status_reference", length=128) private String partnerStatusReference;
    @Column(name="partner_status_transaction_id", length=128) private String partnerStatusTransactionId;
    @Column(name="partner_status_status", length=64) private String partnerExternalStatus;
    @Column(name="partner_status_payment_method", length=64) private String partnerPaymentMethod;
    @Column(name="partner_status_operator_reference", length=128) private String partnerOperatorReference;
    @Column(name="partner_status_debit_effectue") private Boolean partnerDebitEffectue;
    @Column(name="partner_status_quittance_disponible") private Boolean partnerQuittanceDisponible;
    @Column(name="partner_status_provider_updated_at") private Instant partnerProviderUpdatedAt;
    @Column(name="partner_status_failure_reason", length=512) private String partnerFailureReason;
    @Column(name="partner_status_checked_at") private Instant partnerStatusCheckedAt;
    @Column(name="partner_status_request_reference", length=128) private String partnerRequestReference;
    @Column(name="partner_status_correlation_id", length=128) private String partnerCorrelationId;

    @OneToMany(mappedBy="candidate", cascade=CascadeType.ALL, orphanRemoval=true)
    private List<AccountingCandidateEntryJpaEntity> entries = new ArrayList<>();

    protected AccountingCandidateJpaEntity() {}

    static AccountingCandidateJpaEntity create(AccountingCandidateProjection p) {
        var e = new AccountingCandidateJpaEntity();
        e.id=p.id(); e.eventId=p.eventId(); e.paymentId=p.paymentId();
        e.publicPaymentReference=p.publicPaymentReference(); e.partnerId=p.partnerId();
        e.financialInstitutionCode=p.financialInstitutionCode(); e.t0Outcome="COMPLETED";
        e.bankReference=p.bankReference(); e.t0ObservedAt=p.t0ObservedAt();
        e.accountingBusinessDate=p.accountingBusinessDate(); e.financialSnapshotId=p.financialSnapshotId();
        e.financialSnapshotVersion=p.financialSnapshotVersion(); e.financialSnapshotFinalizedAt=p.financialSnapshotFinalizedAt();
        e.debtorAccountReference=p.debtorAccountReference(); e.creditorAccountReference=p.creditorAccountReference();
        e.amount=p.amount(); e.currency=p.currency().getCurrencyCode(); e.paymentOccurredAt=p.paymentOccurredAt();
        e.candidateCreatedAt=p.candidateCreatedAt(); e.batchId=p.batchId();
        p.entries().stream().map(x -> AccountingCandidateEntryJpaEntity.create(e,x)).forEach(e.entries::add);
        if (p.partnerExternalStatusEvidence()!=null) e.recordPartnerExternalEvidence(p.partnerExternalStatusEvidence());
        return e;
    }

    void recordPartnerExternalEvidence(PartnerExternalPaymentStatusEvidence x) {
        partnerStatusReference=x.reference(); partnerStatusTransactionId=x.transactionId();
        partnerExternalStatus=x.providerStatus(); partnerPaymentMethod=x.paymentMethod();
        partnerOperatorReference=x.operatorReference(); partnerDebitEffectue=x.debitEffectue();
        partnerQuittanceDisponible=x.quittanceDisponible(); partnerProviderUpdatedAt=x.providerUpdatedAt();
        partnerFailureReason=x.failureReason(); partnerStatusCheckedAt=x.checkedAt();
        partnerRequestReference=x.requestReference(); partnerCorrelationId=x.correlationId();
    }
    void assignToBatch(UUID batchId) {
        UUID targetBatchId = Objects.requireNonNull(batchId, "batchId");
        if (this.batchId != null && !this.batchId.equals(targetBatchId)) {
            throw new IllegalStateException(
                    "Accounting candidate is already assigned to another batch"
            );
        }
        this.batchId = targetBatchId;
    }
    AccountingCandidateProjection toDomain() {
        PartnerExternalPaymentStatusEvidence x = null;
        if (partnerExternalStatus != null) {
            x = new PartnerExternalPaymentStatusEvidence(
                    partnerStatusReference,partnerStatusTransactionId,partnerExternalStatus,partnerPaymentMethod,
                    partnerOperatorReference,Boolean.TRUE.equals(partnerDebitEffectue),
                    Boolean.TRUE.equals(partnerQuittanceDisponible),partnerProviderUpdatedAt,
                    partnerFailureReason,partnerStatusCheckedAt,partnerRequestReference,partnerCorrelationId
            );
        }
        return new AccountingCandidateProjection(
                id,eventId,paymentId,publicPaymentReference,partnerId,financialInstitutionCode,
                bankReference,t0ObservedAt,accountingBusinessDate,financialSnapshotId,financialSnapshotVersion,
                financialSnapshotFinalizedAt,debtorAccountReference,creditorAccountReference,amount,
                Currency.getInstance(currency),paymentOccurredAt,candidateCreatedAt,batchId,x,
                entries.stream().map(AccountingCandidateEntryJpaEntity::toDomain).toList()
        );
    }
}
