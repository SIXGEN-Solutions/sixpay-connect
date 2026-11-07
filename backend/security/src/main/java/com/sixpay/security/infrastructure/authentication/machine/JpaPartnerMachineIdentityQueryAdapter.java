package com.sixpay.security.infrastructure.authentication.machine;

import com.sixpay.security.application.model.PartnerMachineIdentityView;
import com.sixpay.security.application.port.input.PartnerMachineIdentityQueryUseCase;

import java.util.Objects;
import java.util.Optional;

public final class JpaPartnerMachineIdentityQueryAdapter
        implements PartnerMachineIdentityQueryUseCase {

    private final PartnerMachineIdentitySpringDataRepository repository;

    public JpaPartnerMachineIdentityQueryAdapter(
            PartnerMachineIdentitySpringDataRepository repository
    ) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    public Optional<PartnerMachineIdentityView> findByMachineSubject(
            String machineSubject
    ) {
        if (machineSubject == null || machineSubject.isBlank()) {
            return Optional.empty();
        }

        return repository
                .findByMachineSubjectAndEnabledTrue(machineSubject.strip())
                .map(entity -> new PartnerMachineIdentityView(
                        entity.machineSubject(),
                        entity.partnerIdentifier()
                ));
    }
}
