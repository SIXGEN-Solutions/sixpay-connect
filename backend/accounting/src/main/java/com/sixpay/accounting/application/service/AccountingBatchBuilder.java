package com.sixpay.accounting.application.service;

import com.sixpay.accounting.domain.model.AccountingBatch;
import com.sixpay.accounting.domain.model.AccountingBatchId;
import com.sixpay.accounting.domain.model.AccountingBatchItem;
import com.sixpay.accounting.domain.model.AccountingBatchStatus;
import com.sixpay.accounting.domain.model.AccountingPaymentCandidate;
import com.sixpay.accounting.domain.policy.AccountingEligibilityPolicy;
import com.sixpay.accounting.domain.policy.AccountingSelectionWindow;

import java.time.Clock;
import java.util.List;
import java.util.Objects;

public final class AccountingBatchBuilder {

    private final AccountingEligibilityPolicy eligibilityPolicy;
    private final AccountingBatchIdempotencyKeyFactory idempotencyKeyFactory;
    private final Clock clock;

    public AccountingBatchBuilder(
            AccountingEligibilityPolicy eligibilityPolicy,
            AccountingBatchIdempotencyKeyFactory idempotencyKeyFactory,
            Clock clock
    ) {
        this.eligibilityPolicy = Objects.requireNonNull(
                eligibilityPolicy,
                "eligibilityPolicy"
        );
        this.idempotencyKeyFactory = Objects.requireNonNull(
                idempotencyKeyFactory,
                "idempotencyKeyFactory"
        );
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public AccountingBatch build(
            AccountingSelectionWindow window,
            String financialInstitutionCode,
            List<AccountingPaymentCandidate> candidates
    ) {
        Objects.requireNonNull(window, "window");
        Objects.requireNonNull(candidates, "candidates");

        String institution = required(
                financialInstitutionCode,
                "financialInstitutionCode"
        );

        List<AccountingPaymentCandidate> eligible =
                candidates.stream()
                        .filter(candidate ->
                                candidate.financialInstitutionCode()
                                        .equals(institution)
                        )
                        .filter(candidate ->
                                eligibilityPolicy.evaluate(
                                        candidate,
                                        window
                                ).eligible()
                        )
                        .toList();

        if (eligible.isEmpty()) {
            throw new IllegalArgumentException(
                    "No eligible Payment candidate for accounting batch"
            );
        }

        return new AccountingBatch(
                AccountingBatchId.newId(),
                idempotencyKeyFactory.create(
                        institution,
                        window.businessDate(),
                        eligible
                ),
                window.businessDate(),
                institution,
                clock.instant(),
                AccountingBatchStatus.NOT_COMPLETED,
                eligible.stream()
                        .map(AccountingBatchItem::from)
                        .toList()
        );
    }

    private static String required(
            String value,
            String name
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    name + " is required"
            );
        }
        return value.strip();
    }
}
