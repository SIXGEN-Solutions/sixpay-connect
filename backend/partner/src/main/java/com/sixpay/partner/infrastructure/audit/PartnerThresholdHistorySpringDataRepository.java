package com.sixpay.partner.infrastructure.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PartnerThresholdHistorySpringDataRepository
        extends JpaRepository<PartnerThresholdHistoryJpaEntity, UUID> {
}
