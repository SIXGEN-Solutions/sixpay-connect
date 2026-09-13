package com.sixpay.administration.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DynamicSettingSpringDataRepository
        extends JpaRepository<DynamicSettingJpaEntity, String> {
}
