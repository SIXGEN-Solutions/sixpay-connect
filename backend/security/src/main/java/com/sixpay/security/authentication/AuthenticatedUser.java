package com.sixpay.security.authentication;

import com.sixpay.common.validation.Preconditions;
import com.sixpay.security.authorization.SixpayRole;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Framework-independent representation of an authenticated SIXPAY user.
 *
 * <p>This record is the current concrete implementation of the canonical
 * {@link SixpayPrincipal} contract. Authentication adapters may differ, but
 * authorization and business modules consume the same identity shape.</p>
 *
 * @param subject authenticated subject
 * @param username human-readable username
 * @param authorities granted authorities
 */
public record AuthenticatedUser(
        String subject,
        String username,
        Set<String> authorities
) implements SixpayPrincipal {

    private static final String ROLE_PREFIX = "ROLE_";

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

    /**
     * Returns SIXPAY role names without the Spring Security ROLE_ prefix.
     */
    @Override
    public Set<String> roles() {
        return authorities.stream()
                .filter(authority -> authority.startsWith(ROLE_PREFIX))
                .map(authority -> authority.substring(ROLE_PREFIX.length()))
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Returns granted non-role authorities.
     *
     * <p>This preserves the current SIXPAY scope/authority model while keeping
     * the canonical principal independent from Spring Security token types.</p>
     */
    @Override
    public Set<String> permissions() {
        return authorities.stream()
                .filter(authority -> !authority.startsWith(ROLE_PREFIX))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
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
