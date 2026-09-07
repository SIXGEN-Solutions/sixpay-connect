package com.sixpay.accounting.application.service;

import com.sixpay.accounting.application.port.output.AccountingCandidateProjectionRepository;
import com.sixpay.accounting.application.port.output.PaymentAccountingCandidateSource;
import com.sixpay.accounting.domain.exception.AccountingBatchPersistenceConflictException;
import com.sixpay.accounting.domain.model.AccountingBatch;
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

public final class AccountingBatchConstitutionService {
    private final AccountingCutoffPolicy cutoffPolicy;
    private final PaymentAccountingCandidateSource candidateSource;
    private final AccountingBatchBuilder batchBuilder;
    private final AccountingBatchRepository batchRepository;
    private final AccountingCandidateProjectionRepository candidateRepository;

    public AccountingBatchConstitutionService(
            AccountingCutoffPolicy cutoffPolicy,
            PaymentAccountingCandidateSource candidateSource,
            AccountingBatchBuilder batchBuilder,
            AccountingBatchRepository batchRepository,
            AccountingCandidateProjectionRepository candidateRepository
    ) {
        this.cutoffPolicy = Objects.requireNonNull(cutoffPolicy, "cutoffPolicy");
        this.candidateSource = Objects.requireNonNull(candidateSource, "candidateSource");
        this.batchBuilder = Objects.requireNonNull(batchBuilder, "batchBuilder");
        this.batchRepository = Objects.requireNonNull(batchRepository, "batchRepository");
        this.candidateRepository = Objects.requireNonNull(candidateRepository, "candidateRepository");
    }

    @Transactional
    public AccountingBatch constitute(
            Instant runAt,
            AccountingCutoffMode mode,
            Optional<LocalDate> manualBusinessDate,
            String financialInstitutionCode
    ) {
        AccountingSelectionWindow window = cutoffPolicy.resolve(runAt, mode, manualBusinessDate);
        List<AccountingPaymentCandidate> candidates =
                candidateSource.findUnbatchedStatusVerifiedCandidates(window);
        List<AccountingPaymentCandidate> unassigned = removeAlreadyAssigned(candidates);
        AccountingBatch candidateBatch = batchBuilder.build(window, financialInstitutionCode, unassigned);

        Optional<AccountingBatch> existing = batchRepository.findByIdempotencyKey(candidateBatch.idempotencyKey());
        if (existing.isPresent()) {
            AccountingBatch batch = existing.orElseThrow();
            assignCandidates(batch);
            return batch;
        }

        try {
            AccountingBatch saved = batchRepository.save(candidateBatch);
            assignCandidates(saved);
            return saved;
        } catch (AccountingBatchPersistenceConflictException conflict) {
            AccountingBatch recovered = batchRepository.findByIdempotencyKey(candidateBatch.idempotencyKey())
                    .orElseThrow(() -> conflict);
            assignCandidates(recovered);
            return recovered;
        }
    }

    private void assignCandidates(AccountingBatch batch) {
        batch.items().forEach(item ->
                candidateRepository.assignToBatch(item.paymentId(), batch.batchId().value()));
    }

    private List<AccountingPaymentCandidate> removeAlreadyAssigned(List<AccountingPaymentCandidate> candidates) {
        Objects.requireNonNull(candidates, "candidates");
        Set<UUID> requestedIds = candidates.stream()
                .map(AccountingPaymentCandidate::paymentId)
                .collect(Collectors.toSet());
        if (requestedIds.isEmpty()) return List.of();
        Set<UUID> assignedIds = batchRepository.findAssignedPaymentIds(requestedIds);
        return candidates.stream()
                .filter(candidate -> !assignedIds.contains(candidate.paymentId()))
                .toList();
    }
}
