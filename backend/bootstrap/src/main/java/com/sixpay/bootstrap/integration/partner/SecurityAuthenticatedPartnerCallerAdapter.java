package com.sixpay.bootstrap.integration.partner;

import com.sixpay.payment.application.port.output.partner.AuthenticatedPartnerCallerPort;
import com.sixpay.security.authentication.CurrentMachineIdentityProvider;

import java.util.Objects;
import java.util.Optional;

/**
 * Bootstrap-only composition adapter from Security machine identity to the
 * Payment-owned caller identity port.
 */
public final class SecurityAuthenticatedPartnerCallerAdapter
        implements AuthenticatedPartnerCallerPort {

    private final CurrentMachineIdentityProvider machineIdentityProvider;

    public SecurityAuthenticatedPartnerCallerAdapter(
            CurrentMachineIdentityProvider machineIdentityProvider
    ) {
        this.machineIdentityProvider =
                Objects.requireNonNull(machineIdentityProvider);
    }

    @Override
    public Optional<String> currentAuthenticatedSubject() {
        return machineIdentityProvider
                .currentMachineIdentity()
                .map(identity -> identity.subject());
    }
}
