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

    @Override @Transactional(readOnly=true)
    public Optional<AccountingCandidateProjection> findByEventId(UUID eventId) {
        return repository.findByEventId(eventId).map(AccountingCandidateJpaEntity::toDomain);
    }

    @Override @Transactional(readOnly=true)
    public Optional<AccountingCandidateProjection> findByBusinessIdentity(UUID paymentId, UUID snapshotId) {
        return repository.findByPaymentIdAndFinancialSnapshotId(paymentId,snapshotId).map(AccountingCandidateJpaEntity::toDomain);
    }

    @Override @Transactional
    public AccountingCandidateProjection save(AccountingCandidateProjection p) {
        return repository.save(AccountingCandidateJpaEntity.create(p)).toDomain();
    }

    @Override @Transactional(readOnly=true)
    public List<AccountingCandidateProjection> findEligibleUnbatched(AccountingSelectionWindow w) {
        return repository.findEligibleUnbatched(w.businessDate(),w.fromInclusive(),w.toExclusive())
                .stream().map(AccountingCandidateJpaEntity::toDomain).toList();
    }

    @Override @Transactional
    public void recordTresorPayEvidence(UUID paymentId, TresorPayPaymentStatusEvidence evidence) {
        var e = repository.findByPaymentId(paymentId).orElseThrow(() ->
                new IllegalArgumentException("Accounting candidate not found for paymentId=" + paymentId));
        e.recordTresorPayEvidence(evidence);
    }

    @Override @Transactional
    public void assignToBatch(UUID paymentId, UUID batchId) {
        var e = repository.findByPaymentId(paymentId).orElseThrow(() ->
                new IllegalArgumentException("Accounting candidate not found for paymentId=" + paymentId));
        e.assignToBatch(batchId);
    }

    @Override @Transactional(readOnly=true)
    public List<AccountingPaymentCandidate> findUnbatchedStatusVerifiedCandidates(AccountingSelectionWindow w) {
        return findEligibleUnbatched(w).stream().map(p -> new AccountingPaymentCandidate(
                p.paymentId(),p.publicPaymentReference(),p.partnerId(),p.financialInstitutionCode(),
                p.amount(),p.currency(),p.paymentOccurredAt(),p.accountingBusinessDate(),
                p.bankReference(),p.tresorPayStatusEvidence()
        )).toList();
    }
}
