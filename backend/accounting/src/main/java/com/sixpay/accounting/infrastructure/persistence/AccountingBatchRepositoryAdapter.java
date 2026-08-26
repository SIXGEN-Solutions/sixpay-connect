package com.sixpay.accounting.infrastructure.persistence;

import com.sixpay.accounting.domain.exception.AccountingBatchPersistenceConflictException;
import com.sixpay.accounting.domain.model.AccountingBatch;
import com.sixpay.accounting.domain.model.AccountingBatchId;
import com.sixpay.accounting.domain.model.AccountingBatchIdempotencyKey;
import com.sixpay.accounting.domain.model.AccountingBatchItem;
import com.sixpay.accounting.domain.repository.AccountingBatchRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public class AccountingBatchRepositoryAdapter
        implements AccountingBatchRepository {

    private final AccountingBatchSpringDataRepository repository;

    public AccountingBatchRepositoryAdapter(
            AccountingBatchSpringDataRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public AccountingBatch save(
            AccountingBatch batch
    ) {
        try {
            Optional<AccountingBatchJpaEntity> existing =
                    repository.findAggregateById(
                            batch.batchId().value()
                    );

            AccountingBatchJpaEntity entity;

            if (existing.isPresent()) {
                entity = existing.orElseThrow();
                entity.synchronize(batch);
            } else {
                entity = AccountingBatchJpaEntity.create(
                        batch
                );
            }

            return toDomain(
                    repository.saveAndFlush(entity)
            );
        } catch (DataIntegrityViolationException exception) {
            throw new AccountingBatchPersistenceConflictException(
                    "Accounting batch conflicts with "
                            + "an existing idempotency key "
                            + "or already-assigned Payment",
                    exception
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AccountingBatch> findById(
            AccountingBatchId batchId
    ) {
        return repository
                .findAggregateById(
                        batchId.value()
                )
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AccountingBatch>
    findByIdempotencyKey(
            AccountingBatchIdempotencyKey idempotencyKey
    ) {
        return repository
                .findAggregateByIdempotencyKey(
                        idempotencyKey.value()
                )
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> findAssignedPaymentIds(
            Set<UUID> paymentIds
    ) {
        if (paymentIds.isEmpty()) {
            return Set.of();
        }

        return Set.copyOf(
                repository.findAssignedPaymentIds(
                        paymentIds
                )
        );
    }

    private AccountingBatch toDomain(
            AccountingBatchJpaEntity entity
    ) {
        return new AccountingBatch(
                new AccountingBatchId(
                        entity.id()
                ),
                new AccountingBatchIdempotencyKey(
                        entity.idempotencyKey()
                ),
                entity.businessDate(),
                entity.financialInstitutionCode(),
                entity.createdAt(),
                entity.status(),
                entity.items().stream()
                        .map(item ->
                                new AccountingBatchItem(
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
                                        item.status()
                                )
                        )
                        .toList()
        );
    }
}
