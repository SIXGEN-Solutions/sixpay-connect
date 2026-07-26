package com.sixpay.security.authentication;

import com.sixpay.common.validation.Preconditions;
import com.sixpay.security.authorization.SixpayRole;

import java.util.Set;

/**
 * Framework-independent representation of an authenticated user.
 *
 * @param subject identity provider subject
 * @param username human-readable username
 * @param authorities granted authorities
 */
public record AuthenticatedUser(
        String subject,
        String username,
        Set<String> authorities
) {

    public AuthenticatedUser {
        subject = Preconditions.requireNonBlank(
                subject,
                "Authenticated user subject must not be blank"
        );

        username = Preconditions.requireNonBlank(
                username,
                "Authenticated username must not be blank"
        );

        authorities = Set.copyOf(
                Preconditions.requireNonNull(
                        authorities,
                        "Authorities must not be null"
                )
        );
    }

    public boolean hasAuthority(String authority) {
        String validatedAuthority =
                Preconditions.requireNonBlank(
                        authority,
                        "Authority must not be blank"
                );

        return authorities.contains(validatedAuthority);
    }

    public boolean hasRole(SixpayRole role) {
        SixpayRole validatedRole =
                Preconditions.requireNonNull(
                        role,
                        "Role must not be null"
                );

        return hasAuthority(validatedRole.authority());
    }
}