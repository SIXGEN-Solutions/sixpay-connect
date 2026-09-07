package com.sixpay.accounting.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface AccountingCandidateSpringDataRepository extends JpaRepository<AccountingCandidateJpaEntity, UUID> {
    Optional<AccountingCandidateJpaEntity> findByEventId(UUID eventId);
    Optional<AccountingCandidateJpaEntity> findByPaymentIdAndFinancialSnapshotId(UUID paymentId, UUID financialSnapshotId);
    Optional<AccountingCandidateJpaEntity> findByPaymentId(UUID paymentId);

    @Query("""
            select c from AccountingCandidateJpaEntity c
            where c.batchId is null
              and c.accountingBusinessDate = :businessDate
              and c.paymentOccurredAt >= :fromInclusive
              and c.paymentOccurredAt < :toExclusive
              and upper(c.tresorPayStatus) = 'COMPLETED'
              and c.tresorPayCheckedAt <= :toExclusive
            order by c.paymentOccurredAt asc, c.publicPaymentReference asc
            """)
    List<AccountingCandidateJpaEntity> findEligibleUnbatched(
            @Param("businessDate") LocalDate businessDate,
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive
    );
}
