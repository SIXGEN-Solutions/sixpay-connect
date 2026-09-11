package com.sixpay.accounting.application.service;

import com.sixpay.accounting.application.exception.AccountingProviderRejectedException;
import com.sixpay.accounting.application.exception.AccountingSubmissionOutcomeUnknownException;
import com.sixpay.accounting.application.exception.AccountingT1ManualExecutionException;
import com.sixpay.accounting.application.port.input.AccountingT1ManualExecutionUseCase;
import com.sixpay.accounting.application.port.output.AccountingBatchQueryPort;
import com.sixpay.accounting.application.port.output.AccountingCandidateProjectionRepository;
import com.sixpay.accounting.application.port.output.AccountingIntegrationContext;
import com.sixpay.accounting.domain.model.AccountingBatch;
import com.sixpay.accounting.domain.model.AccountingBatchTracking;
import com.sixpay.accounting.domain.model.AccountingCandidateProjection;
import com.sixpay.accounting.domain.policy.AccountingCutoffMode;
import com.sixpay.accounting.domain.policy.AccountingCutoffPolicy;
import com.sixpay.accounting.domain.policy.AccountingSelectionWindow;
import com.sixpay.accounting.domain.repository.AccountingBatchTrackingRepository;
import com.sixpay.common.context.CorrelationId;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class AccountingT1ManualExecutionService
        implements AccountingT1ManualExecutionUseCase {

    private static final int RECOVERY_PAGE_SIZE = 200;

    private final Clock clock;
    private final AccountingCutoffPolicy cutoffPolicy;
    private final AccountingCandidateProjectionRepository projectionRepository;
    private final AccountingBatchQueryPort queryPort;
    private final AccountingT1OrchestrationService orchestrationService;
    private final AccountingBatchReconciliationService reconciliationService;
    private final AccountingBatchTrackingRepository trackingRepository;

    public AccountingT1ManualExecutionService(
            Clock clock,
            AccountingCutoffPolicy cutoffPolicy,
            AccountingCandidateProjectionRepository projectionRepository,
            AccountingBatchQueryPort queryPort,
            AccountingT1OrchestrationService orchestrationService,
            AccountingBatchReconciliationService reconciliationService,
            AccountingBatchTrackingRepository trackingRepository
    ) {
        this.clock = Objects.requireNonNull(clock);
        this.cutoffPolicy = Objects.requireNonNull(cutoffPolicy);
        this.projectionRepository = Objects.requireNonNull(projectionRepository);
        this.queryPort = Objects.requireNonNull(queryPort);
        this.orchestrationService = Objects.requireNonNull(orchestrationService);
        this.reconciliationService = Objects.requireNonNull(reconciliationService);
        this.trackingRepository = Objects.requireNonNull(trackingRepository);
    }

    @Override
    public Result execute(
            LocalDate businessDate,
            CorrelationId correlationId
    ) {
        Objects.requireNonNull(businessDate, "businessDate");
        Objects.requireNonNull(correlationId, "correlationId");

        Instant runAt = clock.instant();
        AccountingSelectionWindow window = cutoffPolicy.resolve(
                runAt,
                AccountingCutoffMode.MANUAL,
                Optional.of(businessDate)
        );

        List<AccountingCandidateProjection> candidates =
                projectionRepository.findUnbatchedForVerification(window);

        AccountingBatch batch = candidates.isEmpty()
                ? recoverLatestBatch(businessDate)
                : constitute(runAt, businessDate, correlationId, candidates);

        AccountingBatchTracking tracking;
        try {
            tracking = reconciliationService.submitOrReconcile(
                    batch.batchId(),
                    AccountingIntegrationContext.create(correlationId)
            );
        } catch (AccountingSubmissionOutcomeUnknownException
                 | AccountingProviderRejectedException exception) {
            tracking = trackingRepository
                    .findByBatchId(batch.batchId())
                    .orElseThrow(() -> exception);
        }

        AccountingBatch current = queryPort
                .findById(batch.batchId())
                .orElse(batch);

        return new Result(current, tracking);
    }

    private AccountingBatch constitute(
            Instant runAt,
            LocalDate businessDate,
            CorrelationId correlationId,
            List<AccountingCandidateProjection> candidates
    ) {
        Set<String> institutions = candidates.stream()
                .map(AccountingCandidateProjection::financialInstitutionCode)
                .collect(Collectors.toUnmodifiableSet());

        if (institutions.size() != 1) {
            throw new AccountingT1ManualExecutionException(
                    AccountingT1ManualExecutionException.Reason
                            .MULTIPLE_FINANCIAL_INSTITUTIONS,
                    "Manual T1 execution requires exactly one financial institution"
            );
        }

        return orchestrationService.execute(
                runAt,
                AccountingCutoffMode.MANUAL,
                Optional.of(businessDate),
                institutions.iterator().next(),
                correlationId
        );
    }

    private AccountingBatch recoverLatestBatch(
            LocalDate businessDate
    ) {
        return queryPort
                .search(
                        businessDate,
                        null,
                        0,
                        RECOVERY_PAGE_SIZE
                )
                .content()
                .stream()
                .max(Comparator.comparing(AccountingBatch::createdAt))
                .orElseThrow(() ->
                        new AccountingT1ManualExecutionException(
                                AccountingT1ManualExecutionException.Reason
                                        .NO_CANDIDATE_OR_BATCH,
                                "No Accounting candidate or recoverable batch exists for businessDate="
                                        + businessDate
                        )
                );
    }
}
