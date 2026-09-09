package com.sixpay.accounting.infrastructure.tfj.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface AccountingTfjConfirmationSpringDataRepository
        extends JpaRepository<
                AccountingTfjConfirmationJpaEntity,
                UUID
        > {

    Optional<AccountingTfjConfirmationJpaEntity>
    findByIdempotencyKey(String idempotencyKey);

    List<AccountingTfjConfirmationJpaEntity>
    findByMatchedPaymentIdIsNotNullAndFinalityPublishedAtIsNullOrderByReceivedAtAsc(
            Pageable pageable
    );
}
