package com.sixpay.administration.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OperationalIncidentSpringDataRepository
        extends
        JpaRepository<
                OperationalIncidentJpaEntity,
                String
                >,
        JpaSpecificationExecutor<
                OperationalIncidentJpaEntity
                > {
}
