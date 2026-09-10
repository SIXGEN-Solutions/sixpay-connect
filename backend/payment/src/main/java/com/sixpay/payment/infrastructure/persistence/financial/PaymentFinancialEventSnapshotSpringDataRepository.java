package com.sixpay.payment.infrastructure.persistence.financial;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
