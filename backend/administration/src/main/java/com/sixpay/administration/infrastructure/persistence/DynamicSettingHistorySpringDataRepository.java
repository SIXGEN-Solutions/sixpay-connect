package com.sixpay.administration.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DynamicSettingHistorySpringDataRepository
        extends JpaRepository<DynamicSettingHistoryJpaEntity, UUID> {
    List<DynamicSettingHistoryJpaEntity> findByKeyOrderByNewVersionDesc(String key);
    Optional<DynamicSettingHistoryJpaEntity> findByKeyAndNewVersion(String key, long newVersion);
}
