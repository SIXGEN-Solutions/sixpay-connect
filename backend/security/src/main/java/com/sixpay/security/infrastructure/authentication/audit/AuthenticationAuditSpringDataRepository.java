package com.sixpay.security.infrastructure.authentication.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuthenticationAuditSpringDataRepository
        extends JpaRepository<AuthenticationAuditJpaEntity, UUID> {
}
