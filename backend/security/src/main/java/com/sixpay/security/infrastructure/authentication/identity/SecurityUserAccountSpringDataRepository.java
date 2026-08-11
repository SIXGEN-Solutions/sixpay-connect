package com.sixpay.security.infrastructure.authentication.identity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SecurityUserAccountSpringDataRepository
        extends JpaRepository<SecurityUserAccountJpaEntity, UUID> {
}
