package com.sixpay.accounting.infrastructure.persistence;

import com.sixpay.accounting.application.port.output.AccountingCandidateProjectionRepository;
import com.sixpay.accounting.application.port.output.PaymentAccountingCandidateSource;
import com.sixpay.accounting.domain.model.AccountingCandidateProjection;
import com.sixpay.accounting.domain.model.AccountingPaymentCandidate;
import com.sixpay.accounting.domain.model.TresorPayPaymentStatusEvidence;
import com.sixpay.accounting.domain.policy.AccountingSelectionWindow;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AccountingCandidateProjectionRepositoryAdapter
        implements AccountingCandidateProjectionRepository, PaymentAccountingCandidateSource {
    private final AccountingCandidateSpringDataRepository repository;

    public AccountingCandidateProjectionRepositoryAdapter(AccountingCandidateSpringDataRepository repository) {
        this.repository = repository;
    }

    @Override @Transactional(readOnly = true)
    public Optional<AccountingCandidateProjection> findByEventId(UUID eventId) {
        return repository.findByEventId(eventId).map(AccountingCandidateJpaEntity::toDomain);
    }

    @Override @Transactional(readOnly = true)
    public Optional<AccountingCandidateProjection> findByBusinessIdentity(UUID paymentId, UUID snapshotId) {
        return repository.findByPaymentIdAndFinancialSnapshotId(paymentId, snapshotId)
                .map(AccountingCandidateJpaEntity::toDomain);
    }

    @Override @Transactional
    public AccountingCandidateProjection save(AccountingCandidateProjection projection) {
        return repository.save(AccountingCandidateJpaEntity.create(projection)).toDomain();
    }

    @Override @Transactional(readOnly = true)
    public List<AccountingCandidateProjection> findEligibleUnbatched(AccountingSelectionWindow window) {
        return repository.findEligibleUnbatched(window.businessDate(), window.fromInclusive(), window.toExclusive())
                .stream().map(AccountingCandidateJpaEntity::toDomain).toList();
    }

    @Override @Transactional
    public void recordTresorPayEvidence(UUID paymentId, TresorPayPaymentStatusEvidence evidence) {
        var entity = repository.findByPaymentId(paymentId).orElseThrow(() ->
                new IllegalArgumentException("Accounting candidate not found for paymentId=" + paymentId));
        entity.recordTresorPayEvidence(evidence);
    }

    @Override @Transactional
    public void assignToBatch(UUID paymentId, UUID batchId) {
        int updated = repository.assignBatchIfUnassignedOrSame(paymentId, batchId);
        if (updated != 1) {
            throw new IllegalStateException("Accounting candidate already assigned to another batch: " + paymentId);
        }
    }

        @Override
    @Transactional(readOnly = true)
    public List<AccountingPaymentCandidate>
    findUnbatchedStatusVerifiedCandidates(
            AccountingSelectionWindow window
    ) {
        return findEligibleUnbatched(window).stream()
                .map(projection -> new AccountingPaymentCandidate(
                projection.paymentId(),
                projection.publicPaymentReference(),
                projection.partnerId(),
                projection.financialInstitutionCode(),
                projection.financialSnapshotId(),
                projection.financialSnapshotVersion(),
                projection.financialSnapshotFinalizedAt(),
                projection.debtorAccountReference(),
                projection.creditorAccountReference(),
                projection.amount(),
                projection.currency(),
                projection.paymentOccurredAt(),
                projection.accountingBusinessDate(),
                projection.bankReference(),
                projection.tresorPayStatusEvidence(),
                projection.entries().stream()
                        .map(entry -> new AccountingPaymentCandidate.FrozenEntry(
                                entry.entrySnapshotId(),
                                entry.sequence(),
                                entry.direction(),
                                entry.accountReference(),
                                entry.amount(),
                                entry.currency(),
                                entry.createdAt()
                        ))
                        .toList()
        ))
                .toList();
    }
}
