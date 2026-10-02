package com.sixpay.security.application.port.input;

import com.sixpay.security.application.model.PartnerMachineIdentityView;

import java.util.Optional;

/**
 * Resolves a trusted machine subject to the registered Partner identity link.
 */
public interface PartnerMachineIdentityQueryUseCase {

    Optional<PartnerMachineIdentityView> findByMachineSubject(
            String machineSubject
    );
}
