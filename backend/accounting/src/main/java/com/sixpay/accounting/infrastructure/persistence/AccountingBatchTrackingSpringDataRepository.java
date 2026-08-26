package com.sixpay.accounting.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AccountingBatchTrackingSpringDataRepository
        extends JpaRepository<
                AccountingBatchTrackingJpaEntity,
                UUID
        > {

    @Query(
            "select distinct tracking "
                    + "from AccountingBatchTrackingJpaEntity tracking "
                    + "left join fetch tracking.items "
                    + "where tracking.batchId = :batchId"
    )
    Optional<AccountingBatchTrackingJpaEntity>
    findAggregateByBatchId(
            @Param("batchId") UUID batchId
    );
}
