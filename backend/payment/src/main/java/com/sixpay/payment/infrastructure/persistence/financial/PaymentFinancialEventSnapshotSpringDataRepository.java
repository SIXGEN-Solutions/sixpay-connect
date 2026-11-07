package com.sixpay.payment.infrastructure.persistence.financial;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;

import java.util.Optional;
import java.util.UUID;

public interface PaymentFinancialEventSnapshotSpringDataRepository
        extends JpaRepository<
                PaymentFinancialEventSnapshotJpaEntity,
                UUID
        > {

    @EntityGraph(attributePaths = "entries")
    Optional<PaymentFinancialEventSnapshotJpaEntity>
    findByPaymentId(UUID paymentId);

    @Modifying(flushAutomatically = true)
    @Query("""
            update PaymentFinancialEventSnapshotJpaEntity e
               set e.bankEventNumber = :bankEventNumber,
                   e.accountingDate = :accountingDate,
                   e.operationDate = :operationDate
             where e.snapshotId = :snapshotId
               and (e.bankEventNumber is null or e.bankEventNumber = :bankEventNumber)
               and (e.accountingDate is null or e.accountingDate = :accountingDate)
               and (e.operationDate is null or e.operationDate = :operationDate)
            """)
    int enrichCompletedBankingOutcome(
            @Param("snapshotId") UUID snapshotId,
            @Param("bankEventNumber") String bankEventNumber,
            @Param("accountingDate") LocalDate accountingDate,
            @Param("operationDate") LocalDate operationDate
    );
}
