package com.sixpay.partner.infrastructure.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

public interface PartnerAuditSpringDataRepository extends JpaRepository<PartnerAuditJpaEntity, UUID> {

    Page<PartnerAuditJpaEntity> findByPartnerIdAndOccurredAtBetweenOrderByOccurredAtAsc(
            UUID partnerId,
            Instant from,
            Instant to,
            Pageable pageable
    );
}
