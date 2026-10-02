package com.sixpay.security.authentication;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Reads the trusted machine subject established by Spring Security.
 *
 * <p>No Partner business identifier is inferred here. The subject is only the
 * authenticated technical identity key.</p>
 */
public final class SecurityContextCurrentMachineIdentityProvider
        implements CurrentMachineIdentityProvider {

    @Override
    public Optional<AuthenticatedMachineIdentity> currentMachineIdentity() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            return Optional.empty();
        }

        /*
         * Human SIXPAY principals are deliberately excluded from the M2M
         * surface. Partner system calls must be represented by the transport
         * authentication itself, not by AuthenticatedUser.
         */
        if (authentication.getPrincipal() instanceof AuthenticatedUser) {
            return Optional.empty();
        }

        return Optional.of(
                new AuthenticatedMachineIdentity(authentication.getName())
        );
    }
}
