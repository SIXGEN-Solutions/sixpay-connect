package com.sixpay.accounting.infrastructure.persistence;

import com.sixpay.accounting.domain.model.AccountingCandidateProjection;
import com.sixpay.accounting.domain.model.TresorPayPaymentStatusEvidence;
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

    @Column(name="tresorpay_reference", length=128) private String tresorPayReference;
    @Column(name="tresorpay_transaction_id", length=128) private String tresorPayTransactionId;
    @Column(name="tresorpay_status", length=64) private String tresorPayStatus;
    @Column(name="tresorpay_payment_method", length=64) private String tresorPayPaymentMethod;
    @Column(name="tresorpay_operator_reference", length=128) private String tresorPayOperatorReference;
    @Column(name="tresorpay_debit_effectue") private Boolean tresorPayDebitEffectue;
    @Column(name="tresorpay_quittance_disponible") private Boolean tresorPayQuittanceDisponible;
    @Column(name="tresorpay_provider_updated_at") private Instant tresorPayProviderUpdatedAt;
    @Column(name="tresorpay_failure_reason", length=512) private String tresorPayFailureReason;
    @Column(name="tresorpay_checked_at") private Instant tresorPayCheckedAt;
    @Column(name="tresorpay_request_reference", length=128) private String tresorPayRequestReference;
    @Column(name="tresorpay_correlation_id", length=128) private String tresorPayCorrelationId;

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
        if (p.tresorPayStatusEvidence()!=null) e.recordTresorPayEvidence(p.tresorPayStatusEvidence());
        return e;
    }

    void recordTresorPayEvidence(TresorPayPaymentStatusEvidence x) {
        tresorPayReference=x.reference(); tresorPayTransactionId=x.transactionId();
        tresorPayStatus=x.providerStatus(); tresorPayPaymentMethod=x.paymentMethod();
        tresorPayOperatorReference=x.operatorReference(); tresorPayDebitEffectue=x.debitEffectue();
        tresorPayQuittanceDisponible=x.quittanceDisponible(); tresorPayProviderUpdatedAt=x.providerUpdatedAt();
        tresorPayFailureReason=x.failureReason(); tresorPayCheckedAt=x.checkedAt();
        tresorPayRequestReference=x.requestReference(); tresorPayCorrelationId=x.correlationId();
    }
    void assignToBatch(UUID batchId) {
        Objects.requireNonNull(batchId, "batchId");
        if (this.batchId == null) {
            this.batchId = batchId;
            return;
        }
        if (!this.batchId.equals(batchId)) {
            throw new IllegalStateException(
                    "Accounting candidate is already assigned to another batch"
            );
        }
    }
    AccountingCandidateProjection toDomain() {
        TresorPayPaymentStatusEvidence x = null;
        if (tresorPayStatus != null) {
            x = new TresorPayPaymentStatusEvidence(
                    tresorPayReference,tresorPayTransactionId,tresorPayStatus,tresorPayPaymentMethod,
                    tresorPayOperatorReference,Boolean.TRUE.equals(tresorPayDebitEffectue),
                    Boolean.TRUE.equals(tresorPayQuittanceDisponible),tresorPayProviderUpdatedAt,
                    tresorPayFailureReason,tresorPayCheckedAt,tresorPayRequestReference,tresorPayCorrelationId
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
