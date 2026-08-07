package com.sixpay.accounting.application.service;

import com.sixpay.accounting.application.exception.AccountingBatchNotFoundException;
import com.sixpay.accounting.application.exception.AccountingProviderRejectedException;
import com.sixpay.accounting.application.exception.AccountingSubmissionOutcomeUnknownException;
import com.sixpay.accounting.application.port.output.AccountingBatchGateway;
import com.sixpay.accounting.application.port.output.AccountingIntegrationContext;
import com.sixpay.accounting.domain.model.AccountingBatch;
import com.sixpay.accounting.domain.model.AccountingBatchId;
import com.sixpay.accounting.domain.model.AccountingBatchItem;
import com.sixpay.accounting.domain.model.AccountingBatchItemStatus;
import com.sixpay.accounting.domain.model.AccountingBatchItemTracking;
import com.sixpay.accounting.domain.model.AccountingBatchStatus;
import com.sixpay.accounting.domain.model.AccountingBatchTracking;
import com.sixpay.accounting.domain.model.AccountingProviderBatchResult;
import com.sixpay.accounting.domain.model.AccountingProviderItemResult;
import com.sixpay.accounting.domain.model.AccountingSubmissionState;
import com.sixpay.accounting.domain.repository.AccountingBatchRepository;
import com.sixpay.accounting.domain.repository.AccountingBatchTrackingRepository;
import com.sixpay.accounting.domain.repository.AccountingReconciliationRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class AccountingBatchReconciliationService {

    private final AccountingBatchRepository batchRepository;
    private final AccountingBatchTrackingRepository trackingRepository;
    private final AccountingReconciliationRepository reconciliationRepository;
    private final AccountingBatchGateway gateway;
    private final Clock clock;

    public AccountingBatchReconciliationService(
            AccountingBatchRepository batchRepository,
            AccountingBatchTrackingRepository trackingRepository,
            AccountingReconciliationRepository reconciliationRepository,
            AccountingBatchGateway gateway,
            Clock clock
    ) {
        this.batchRepository = Objects.requireNonNull(
                batchRepository,
                "batchRepository"
        );
        this.trackingRepository = Objects.requireNonNull(
                trackingRepository,
                "trackingRepository"
        );
        this.reconciliationRepository = Objects.requireNonNull(
                reconciliationRepository,
                "reconciliationRepository"
        );
        this.gateway = Objects.requireNonNull(
                gateway,
                "gateway"
        );
        this.clock = Objects.requireNonNull(
                clock,
                "clock"
        );
    }

    public AccountingBatchTracking submitOrReconcile(
            AccountingBatchId batchId,
            AccountingIntegrationContext context
    ) {
        AccountingBatch batch = requireBatch(batchId);
        AccountingBatchTracking tracking =
                trackingRepository.findByBatchId(batchId)
                        .orElseGet(() ->
                                AccountingBatchTracking.ready(
                                        batchId
                                )
                        );

        if (tracking.submissionState()
                == AccountingSubmissionState.COMPLETED
                || tracking.submissionState()
                == AccountingSubmissionState.REJECTED) {
            return tracking;
        }

        if (tracking.submissionState()
                != AccountingSubmissionState.READY) {
            return reconcile(
                    batchId,
                    context
            );
        }

        Instant now = clock.instant();

        reconciliationRepository.saveTracking(
                tracking.submissionAttempted(now)
        );

        try {
            AccountingProviderBatchResult result =
                    gateway.submit(
                            batch,
                            context
                    );

            return applyResult(
                    batch,
                    tracking,
                    result,
                    now,
                    false
            );
        } catch (AccountingSubmissionOutcomeUnknownException exception) {
            reconciliationRepository.saveTracking(
                    tracking.outcomeUnknown(
                            now,
                            "SUBMISSION_OUTCOME_UNKNOWN"
                    )
            );
            throw exception;
        } catch (AccountingProviderRejectedException exception) {
            reconciliationRepository.saveTracking(
                    tracking.rejected(
                            now,
                            "HTTP_" + exception.statusCode()
                    )
            );
            throw exception;
        }
    }

    public AccountingBatchTracking reconcile(
            AccountingBatchId batchId,
            AccountingIntegrationContext context
    ) {
        AccountingBatch batch = requireBatch(batchId);
        AccountingBatchTracking tracking =
                trackingRepository.findByBatchId(batchId)
                        .orElseGet(() ->
                                AccountingBatchTracking.ready(
                                        batchId
                                )
                        );

        if (tracking.submissionState()
                == AccountingSubmissionState.COMPLETED
                || tracking.submissionState()
                == AccountingSubmissionState.REJECTED) {
            return tracking;
        }

        Optional<AccountingProviderBatchResult> result =
                gateway.findByIdempotencyKey(
                        batch.idempotencyKey(),
                        context
                );

        if (result.isEmpty()) {
            result = gateway.findByBatchId(
                    batch.batchId(),
                    context
            );
        }

        Instant now = clock.instant();

        if (result.isEmpty()) {
            return reconciliationRepository.saveTracking(
                    tracking.reconciliationMiss(now)
            );
        }

        return applyResult(
                batch,
                tracking,
                result.orElseThrow(),
                now,
                true
        );
    }

    private AccountingBatchTracking applyResult(
            AccountingBatch batch,
            AccountingBatchTracking tracking,
            AccountingProviderBatchResult result,
            Instant now,
            boolean reconciliation
    ) {
        Map<java.util.UUID, AccountingProviderItemResult>
                providerItems =
                result.items().stream()
                        .collect(
                                Collectors.toMap(
                                        AccountingProviderItemResult::paymentId,
                                        Function.identity()
                                )
                        );

        List<AccountingBatchItem> updatedItems =
                batch.items().stream()
                        .map(item ->
                                updateItem(
                                        item,
                                        providerItems.get(
                                                item.paymentId()
                                        )
                                )
                        )
                        .toList();

        AccountingBatch updatedBatch =
                new AccountingBatch(
                        batch.batchId(),
                        batch.idempotencyKey(),
                        batch.businessDate(),
                        batch.financialInstitutionCode(),
                        batch.createdAt(),
                        result.status(),
                        updatedItems
                );

        AccountingSubmissionState state =
                resolveSubmissionState(
                        result
                );

        List<AccountingBatchItemTracking> itemTracking =
                result.items().stream()
                        .map(item ->
                                new AccountingBatchItemTracking(
                                        item.paymentId(),
                                        item.providerItemReference(),
                                        item.rejectionCode(),
                                        now
                                )
                        )
                        .toList();

        AccountingBatchTracking updatedTracking =
                reconciliation
                        ? tracking.reconciled(
                                state,
                                result.providerBatchReference(),
                                now,
                                itemTracking
                        )
                        : tracking.submissionResolved(
                                state,
                                result.providerBatchReference(),
                                now,
                                itemTracking
                        );

        return reconciliationRepository.saveResult(
                updatedBatch,
                updatedTracking
        );
    }

    private static AccountingBatchItem updateItem(
            AccountingBatchItem item,
            AccountingProviderItemResult providerItem
    ) {
        if (providerItem == null) {
            return item;
        }

        return new AccountingBatchItem(
                item.paymentId(),
                item.publicPaymentReference(),
                item.partnerId(),
                item.amount(),
                item.currency(),
                item.paymentOccurredAt(),
                item.paymentBusinessDate(),
                item.bankPostingReference(),
                item.tresorPayStatus(),
                item.tresorPayStatusCheckedAt(),
                providerItem.status()
        );
    }

    private static AccountingSubmissionState
    resolveSubmissionState(
            AccountingProviderBatchResult result
    ) {
        if (result.status()
                == AccountingBatchStatus.COMPLETED) {
            return AccountingSubmissionState.COMPLETED;
        }

        if (result.items().stream()
                .anyMatch(item ->
                        item.status()
                                == AccountingBatchItemStatus
                                .RECONCILIATION_REQUIRED
                )) {
            return AccountingSubmissionState
                    .RECONCILIATION_REQUIRED;
        }

        if (!result.items().isEmpty()
                && result.items().stream()
                .allMatch(item ->
                        item.status()
                                == AccountingBatchItemStatus.REJECTED
                )) {
            return AccountingSubmissionState.REJECTED;
        }

        return AccountingSubmissionState.SUBMITTED;
    }

    private AccountingBatch requireBatch(
            AccountingBatchId batchId
    ) {
        return batchRepository.findById(
                        Objects.requireNonNull(
                                batchId,
                                "batchId"
                        )
                )
                .orElseThrow(
                        () -> new AccountingBatchNotFoundException(
                                batchId
                        )
                );
    }
}
