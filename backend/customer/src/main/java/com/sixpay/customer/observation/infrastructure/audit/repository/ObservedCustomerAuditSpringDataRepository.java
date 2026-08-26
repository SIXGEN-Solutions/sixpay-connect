package com.sixpay.customer.observation.infrastructure.audit.repository;

import com.sixpay.customer.observation.infrastructure.audit.entity
        .ObservedCustomerAuditJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ObservedCustomerAuditSpringDataRepository
        extends JpaRepository<ObservedCustomerAuditJpaEntity, UUID> {
}
