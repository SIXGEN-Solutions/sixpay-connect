package com.sixpay.accounting.application.service;

import com.sixpay.accounting.application.port.output.AccountingCandidateProjectionRepository;
import com.sixpay.accounting.application.port.output.PaymentAccountingCandidateSource;
import com.sixpay.accounting.domain.exception.AccountingBatchPersistenceConflictException;
import com.sixpay.accounting.domain.model.AccountingBatch;
import com.sixpay.accounting.domain.model.AccountingCandidateProjection;
import com.sixpay.accounting.domain.model.AccountingPaymentCandidate;
import com.sixpay.accounting.domain.policy.AccountingCutoffMode;
import com.sixpay.accounting.domain.policy.AccountingCutoffPolicy;
import com.sixpay.accounting.domain.policy.AccountingSelectionWindow;
import com.sixpay.accounting.domain.repository.AccountingBatchRepository;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class AccountingBatchConstitutionService {
    private final AccountingCutoffPolicy cutoffPolicy;
    private final PaymentAccountingCandidateSource legacyCandidateSource;
    private final AccountingCandidateProjectionRepository projectionRepository;
    private final AccountingBatchBuilder batchBuilder;
    private final AccountingBatchRepository batchRepository;

    public AccountingBatchConstitutionService(AccountingCutoffPolicy cutoffPolicy,
                                               AccountingCandidateProjectionRepository projectionRepository,
                                               AccountingBatchBuilder batchBuilder,
                                               AccountingBatchRepository batchRepository) {
        this.cutoffPolicy = Objects.requireNonNull(cutoffPolicy, "cutoffPolicy");
        this.legacyCandidateSource = null;
        this.projectionRepository = Objects.requireNonNull(projectionRepository, "projectionRepository");
        this.batchBuilder = Objects.requireNonNull(batchBuilder, "batchBuilder");
        this.batchRepository = Objects.requireNonNull(batchRepository, "batchRepository");
    }

    /** Legacy constructor retained for historical tests only. */
    public AccountingBatchConstitutionService(AccountingCutoffPolicy cutoffPolicy,
                                               PaymentAccountingCandidateSource candidateSource,
                                               AccountingBatchBuilder batchBuilder,
                                               AccountingBatchRepository batchRepository) {
        this.cutoffPolicy = Objects.requireNonNull(cutoffPolicy, "cutoffPolicy");
        this.legacyCandidateSource = Objects.requireNonNull(candidateSource, "candidateSource");
        this.projectionRepository = null;
        this.batchBuilder = Objects.requireNonNull(batchBuilder, "batchBuilder");
        this.batchRepository = Objects.requireNonNull(batchRepository, "batchRepository");
    }

    @Transactional
    public AccountingBatch constitute(Instant runAt, AccountingCutoffMode mode,
                                      Optional<LocalDate> manualBusinessDate,
                                      String financialInstitutionCode) {
        AccountingSelectionWindow window = cutoffPolicy.resolve(runAt, mode, manualBusinessDate);
        if (projectionRepository != null) {
            return constituteFromProjection(window, financialInstitutionCode);
        }
        return constituteLegacy(window, financialInstitutionCode);
    }

    private AccountingBatch constituteFromProjection(AccountingSelectionWindow window, String institution) {
        List<AccountingCandidateProjection> candidates = projectionRepository.findEligibleUnbatched(window);
        List<AccountingCandidateProjection> unassigned = removeAssignedProjections(candidates);
        AccountingBatch candidateBatch = batchBuilder.buildFromProjections(window, institution, unassigned);
        Optional<AccountingBatch> existing = batchRepository.findByIdempotencyKey(candidateBatch.idempotencyKey());
        if (existing.isPresent()) {
            AccountingBatch batch = existing.orElseThrow();
            assign(unassigned, batch.batchId().value());
            return batch;
        }
        try {
            AccountingBatch saved = batchRepository.save(candidateBatch);
            assign(unassigned, saved.batchId().value());
            return saved;
        } catch (AccountingBatchPersistenceConflictException conflict) {
            AccountingBatch batch = batchRepository.findByIdempotencyKey(candidateBatch.idempotencyKey())
                    .orElseThrow(() -> conflict);
            assign(unassigned, batch.batchId().value());
            return batch;
        }
    }

    private void assign(List<AccountingCandidateProjection> candidates, UUID batchId) {
        for (AccountingCandidateProjection candidate : candidates) {
            projectionRepository.assignToBatch(candidate.paymentId(), batchId);
        }
    }

    private List<AccountingCandidateProjection> removeAssignedProjections(List<AccountingCandidateProjection> candidates) {
        Set<UUID> ids = candidates.stream().map(AccountingCandidateProjection::paymentId).collect(Collectors.toSet());
        if (ids.isEmpty()) return List.of();
        Set<UUID> assigned = batchRepository.findAssignedPaymentIds(ids);
        return candidates.stream().filter(c -> !assigned.contains(c.paymentId())).toList();
    }

    private AccountingBatch constituteLegacy(AccountingSelectionWindow window, String institution) {
        List<AccountingPaymentCandidate> candidates = legacyCandidateSource.findUnbatchedStatusVerifiedCandidates(window);
        Set<UUID> ids = candidates.stream().map(AccountingPaymentCandidate::paymentId).collect(Collectors.toSet());
        Set<UUID> assigned = ids.isEmpty() ? Set.of() : batchRepository.findAssignedPaymentIds(ids);
        List<AccountingPaymentCandidate> unassigned = candidates.stream()
                .filter(c -> !assigned.contains(c.paymentId())).toList();
        AccountingBatch candidateBatch = batchBuilder.build(window, institution, unassigned);
        Optional<AccountingBatch> existing = batchRepository.findByIdempotencyKey(candidateBatch.idempotencyKey());
        if (existing.isPresent()) return existing.orElseThrow();
        try {
            return batchRepository.save(candidateBatch);
        } catch (AccountingBatchPersistenceConflictException conflict) {
            return batchRepository.findByIdempotencyKey(candidateBatch.idempotencyKey())
                    .orElseThrow(() -> conflict);
        }
    }
}
