package com.sixpay.accounting.infrastructure.persistence;

import com.sixpay.accounting.domain.model.AccountingBatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface AccountingBatchSpringDataRepository
        extends JpaRepository<AccountingBatchJpaEntity, UUID> {

    @Query(
            "select distinct batch "
                    + "from AccountingBatchJpaEntity batch "
                    + "left join fetch batch.items "
                    + "where batch.id = :id"
    )
    Optional<AccountingBatchJpaEntity> findAggregateById(
            @Param("id") UUID id
    );

    @Query(
            "select distinct batch "
                    + "from AccountingBatchJpaEntity batch "
                    + "left join fetch batch.items "
                    + "where batch.idempotencyKey = :idempotencyKey"
    )
    Optional<AccountingBatchJpaEntity>
    findAggregateByIdempotencyKey(
            @Param("idempotencyKey")
            String idempotencyKey
    );

    Page<AccountingBatchJpaEntity>
    findAllByBusinessDateAndStatus(
            LocalDate businessDate,
            AccountingBatchStatus status,
            Pageable pageable
    );

    Page<AccountingBatchJpaEntity>
    findAllByBusinessDate(
            LocalDate businessDate,
            Pageable pageable
    );

    Page<AccountingBatchJpaEntity>
    findAllByStatus(
            AccountingBatchStatus status,
            Pageable pageable
    );

    @Query(
            "select item.paymentId "
                    + "from AccountingBatchItemJpaEntity item "
                    + "where item.paymentId in :paymentIds"
    )
    Set<UUID> findAssignedPaymentIds(
            @Param("paymentIds")
            Collection<UUID> paymentIds
    );
}