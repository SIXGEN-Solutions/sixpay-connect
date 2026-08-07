package com.sixpay.accounting.infrastructure.persistence;

import com.sixpay.accounting.domain.model.AccountingBatch;
import com.sixpay.accounting.domain.model.AccountingBatchId;
import com.sixpay.accounting.domain.model.AccountingBatchItemTracking;
import com.sixpay.accounting.domain.model.AccountingBatchTracking;
import com.sixpay.accounting.domain.repository.AccountingBatchRepository;
import com.sixpay.accounting.domain.repository.AccountingBatchTrackingRepository;
import com.sixpay.accounting.domain.repository.AccountingReconciliationRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Repository
public class AccountingBatchTrackingRepositoryAdapter
        implements AccountingBatchTrackingRepository,
        AccountingReconciliationRepository {

    private final AccountingBatchTrackingSpringDataRepository
            trackingRepository;

    private final AccountingBatchRepository
            batchRepository;

    public AccountingBatchTrackingRepositoryAdapter(
            AccountingBatchTrackingSpringDataRepository
                    trackingRepository,
            AccountingBatchRepository batchRepository
    ) {
        this.trackingRepository =
                Objects.requireNonNull(
                        trackingRepository,
                        "trackingRepository"
                );

        this.batchRepository =
                Objects.requireNonNull(
                        batchRepository,
                        "batchRepository"
                );
    }

    @Override
    @Transactional
    public AccountingBatchTracking save(
            AccountingBatchTracking tracking
    ) {
        Objects.requireNonNull(
                tracking,
                "tracking"
        );

        AccountingBatchTrackingJpaEntity entity =
                trackingRepository
                        .findAggregateByBatchId(
                                tracking
                                        .batchId()
                                        .value()
                        )
                        .orElseGet(() ->
                                AccountingBatchTrackingJpaEntity
                                        .create(
                                                tracking
                                        )
                        );

        entity.synchronize(
                tracking
        );

        AccountingBatchTrackingJpaEntity saved =
                trackingRepository
                        .saveAndFlush(
                                entity
                        );

        return toDomain(
                saved
        );
    }

    @Override
    @Transactional
    public AccountingBatchTracking saveTracking(
            AccountingBatchTracking tracking
    ) {
        return save(
                tracking
        );
    }

    @Override
    @Transactional
    public AccountingBatchTracking saveResult(
            AccountingBatch batch,
            AccountingBatchTracking tracking
    ) {
        Objects.requireNonNull(
                batch,
                "batch"
        );

        Objects.requireNonNull(
                tracking,
                "tracking"
        );

        if (!batch.batchId()
                .equals(
                        tracking.batchId()
                )) {
            throw new IllegalArgumentException(
                    "Batch and tracking must reference "
                            + "the same accounting batch"
            );
        }

        batchRepository.save(
                batch
        );

        return save(
                tracking
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AccountingBatchTracking>
    findByBatchId(
            AccountingBatchId batchId
    ) {
        Objects.requireNonNull(
                batchId,
                "batchId"
        );

        return trackingRepository
                .findAggregateByBatchId(
                        batchId.value()
                )
                .map(
                        this::toDomain
                );
    }

    private AccountingBatchTracking toDomain(
            AccountingBatchTrackingJpaEntity entity
    ) {
        return new AccountingBatchTracking(
                new AccountingBatchId(
                        entity.batchId()
                ),
                entity.submissionState(),
                entity.providerBatchReference(),
                entity.lastSubmissionAttemptAt(),
                entity.lastReconciliationAt(),
                entity.reconciliationAttempts(),
                entity.lastErrorCode(),
                entity.items()
                        .stream()
                        .map(item ->
                                new AccountingBatchItemTracking(
                                        item.paymentId(),
                                        item.providerItemReference(),
                                        item.rejectionCode(),
                                        item.updatedAt()
                                )
                        )
                        .toList()
        );
    }
}