package com.sixpay.partner.infrastructure.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OutboxEventSpringDataRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {
}
