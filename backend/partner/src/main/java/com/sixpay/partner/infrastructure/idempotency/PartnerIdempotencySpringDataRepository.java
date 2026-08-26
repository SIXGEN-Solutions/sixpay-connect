package com.sixpay.partner.infrastructure.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PartnerIdempotencySpringDataRepository
        extends JpaRepository<PartnerIdempotencyJpaEntity, UUID> {

    Optional<PartnerIdempotencyJpaEntity> findByOperationAndIdempotencyKey(
            String operation,
            String idempotencyKey
    );
}
