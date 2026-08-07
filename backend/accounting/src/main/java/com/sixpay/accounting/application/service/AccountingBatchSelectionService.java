package com.sixpay.accounting.application.service;

import com.sixpay.accounting.application.port.output.PaymentAccountingCandidateSource;
import com.sixpay.accounting.domain.model.AccountingBatch;
import com.sixpay.accounting.domain.policy.AccountingCutoffMode;
import com.sixpay.accounting.domain.policy.AccountingCutoffPolicy;
import com.sixpay.accounting.domain.policy.AccountingSelectionWindow;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

public final class AccountingBatchSelectionService {

    private final AccountingCutoffPolicy cutoffPolicy;
    private final PaymentAccountingCandidateSource candidateSource;
    private final AccountingBatchBuilder batchBuilder;

    public AccountingBatchSelectionService(
            AccountingCutoffPolicy cutoffPolicy,
            PaymentAccountingCandidateSource candidateSource,
            AccountingBatchBuilder batchBuilder
    ) {
        this.cutoffPolicy = Objects.requireNonNull(
                cutoffPolicy,
                "cutoffPolicy"
        );
        this.candidateSource = Objects.requireNonNull(
                candidateSource,
                "candidateSource"
        );
        this.batchBuilder = Objects.requireNonNull(
                batchBuilder,
                "batchBuilder"
        );
    }

    public AccountingBatch select(
            Instant runAt,
            AccountingCutoffMode mode,
            Optional<LocalDate> manualBusinessDate,
            String financialInstitutionCode
    ) {
        AccountingSelectionWindow window =
                cutoffPolicy.resolve(
                        runAt,
                        mode,
                        manualBusinessDate
                );

        return batchBuilder.build(
                window,
                financialInstitutionCode,
                candidateSource
                        .findUnbatchedStatusVerifiedCandidates(
                                window
                        )
        );
    }
}
