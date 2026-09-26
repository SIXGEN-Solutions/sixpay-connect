package com.sixpay.security.authentication;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

import java.util.Optional;

/**
 * Security public surface for the authenticated machine caller.
 */
public interface CurrentMachineIdentityProvider {

    Optional<AuthenticatedMachineIdentity> currentMachineIdentity();

    default AuthenticatedMachineIdentity requireCurrentMachineIdentity() {
        return currentMachineIdentity().orElseThrow(
                () -> new AuthenticationCredentialsNotFoundException(
                        "No authenticated machine identity is available"
                )
        );
    }
}
