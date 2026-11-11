package com.sixpay.payment.infrastructure.persistence.financial;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.UUID;

interface PaymentFinancialEntrySnapshotSpringDataRepository
        extends JpaRepository<PaymentFinancialEntrySnapshotJpaEntity, UUID> {

    @Modifying(flushAutomatically = true)
    @Query("""
            update PaymentFinancialEntrySnapshotJpaEntity e
               set e.bkmvti_dco = :accountingDate
             where e.eventSnapshot.snapshotId = :snapshotId
               and (e.bkmvti_dco is null or e.bkmvti_dco = :accountingDate)
            """)
    int enrichAccountingDate(
            @Param("snapshotId") UUID snapshotId,
            @Param("accountingDate") LocalDate accountingDate
    );
}
