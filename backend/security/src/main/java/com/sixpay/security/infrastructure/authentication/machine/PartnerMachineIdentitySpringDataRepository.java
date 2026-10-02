package com.sixpay.security.infrastructure.authentication.machine;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PartnerMachineIdentitySpringDataRepository
        extends JpaRepository<PartnerMachineIdentityJpaEntity, UUID> {

    Optional<PartnerMachineIdentityJpaEntity> findByMachineSubjectAndEnabledTrue(
            String machineSubject
    );
}
