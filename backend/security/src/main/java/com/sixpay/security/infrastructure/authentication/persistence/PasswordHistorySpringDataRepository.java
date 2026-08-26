package com.sixpay.security.infrastructure.authentication.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PasswordHistorySpringDataRepository
        extends JpaRepository<PasswordHistoryJpaEntity, UUID> {
    List<PasswordHistoryJpaEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
