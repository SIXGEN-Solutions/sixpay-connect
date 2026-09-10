package com.sixpay.accounting.application.service;

import com.sixpay.accounting.application.port.output.AccountingCandidateProjectionRepository;
import com.sixpay.accounting.application.port.output.AccountingIntegrationContext;
import com.sixpay.accounting.application.port.output.TresorPayPaymentStatusGateway;
import com.sixpay.accounting.domain.model.AccountingBatch;
import com.sixpay.accounting.domain.model.AccountingCandidateProjection;
import com.sixpay.accounting.domain.policy.AccountingCutoffMode;
import com.sixpay.accounting.domain.policy.AccountingCutoffPolicy;
import com.sixpay.accounting.domain.policy.AccountingSelectionWindow;
import com.sixpay.common.context.CorrelationId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

public final class AccountingT1OrchestrationService {

    private final AccountingCutoffPolicy cutoffPolicy;
    private final AccountingCandidateProjectionRepository projectionRepository;
    private final TresorPayPaymentStatusGateway tresorPayGateway;
    private final AccountingBatchConstitutionService constitutionService;

    public AccountingT1OrchestrationService(
            AccountingCutoffPolicy cutoffPolicy,
            AccountingCandidateProjectionRepository projectionRepository,
            TresorPayPaymentStatusGateway tresorPayGateway,
            AccountingBatchConstitutionService constitutionService
    ) {
        this.cutoffPolicy = Objects.requireNonNull(cutoffPolicy);
        this.projectionRepository = Objects.requireNonNull(projectionRepository);
        this.tresorPayGateway = Objects.requireNonNull(tresorPayGateway);
        this.constitutionService = Objects.requireNonNull(constitutionService);
    }

    public AccountingBatch execute(
            Instant runAt,
            AccountingCutoffMode mode,
            Optional<LocalDate> manualBusinessDate,
            String financialInstitutionCode,
            CorrelationId correlationId
    ) {
        Objects.requireNonNull(runAt, "runAt");
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(manualBusinessDate, "manualBusinessDate");
        Objects.requireNonNull(correlationId, "correlationId");

        AccountingSelectionWindow window =
                cutoffPolicy.resolve(runAt, mode, manualBusinessDate);

        for (AccountingCandidateProjection candidate :
                projectionRepository.findUnbatchedForVerification(window)) {

            var evidence =
                    tresorPayGateway.findByPaymentReference(
                            candidate.publicPaymentReference(),
                            AccountingIntegrationContext.create(correlationId)
                    );

            projectionRepository.recordTresorPayEvidence(
                    candidate.paymentId(),
                    evidence
            );
        }

        return constitutionService.constitute(
                runAt,
                mode,
                manualBusinessDate,
                financialInstitutionCode
        );
    }
}
