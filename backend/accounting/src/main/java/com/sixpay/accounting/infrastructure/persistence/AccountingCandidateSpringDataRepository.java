package com.sixpay.accounting.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
    Optional<AccountingCandidateJpaEntity> findById(UUID id);

    org.springframework.data.domain.Page<AccountingCandidateJpaEntity>
    findByAccountingBusinessDateAndPublicPaymentReferenceContainingIgnoreCase(
            LocalDate businessDate,
            String publicPaymentReference,
            org.springframework.data.domain.Pageable pageable
    );

    org.springframework.data.domain.Page<AccountingCandidateJpaEntity>
    findByAccountingBusinessDate(
            LocalDate businessDate,
            org.springframework.data.domain.Pageable pageable
    );

    org.springframework.data.domain.Page<AccountingCandidateJpaEntity>
    findByPublicPaymentReferenceContainingIgnoreCase(
            String publicPaymentReference,
            org.springframework.data.domain.Pageable pageable
    );


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
            @Param("toExclusive") Instant toExclusive);

    @Query("""
            select c from AccountingCandidateJpaEntity c
            where c.batchId is null
              and c.accountingBusinessDate = :businessDate
              and c.paymentOccurredAt >= :fromInclusive
              and c.paymentOccurredAt < :toExclusive
              and (
                    c.tresorPayStatus is null
                    or c.tresorPayCheckedAt is null
                    or c.tresorPayCheckedAt > :toExclusive
                  )
            order by c.paymentOccurredAt asc, c.publicPaymentReference asc
            """)
    List<AccountingCandidateJpaEntity> findUnbatchedForVerification(
            @Param("businessDate") LocalDate businessDate,
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update AccountingCandidateJpaEntity c
               set c.batchId = :batchId
             where c.paymentId = :paymentId
               and (c.batchId is null or c.batchId = :batchId)
            """)
    int assignBatchIfUnassignedOrSame(@Param("paymentId") UUID paymentId,
                                      @Param("batchId") UUID batchId);
}
