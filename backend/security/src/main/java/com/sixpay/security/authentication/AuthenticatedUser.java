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
 * <p>{@code passwordChangeRequired} is meaningful only for LOCAL
 * authentication. OIDC principals use the compatibility constructor and
 * therefore always carry {@code false}: their password lifecycle belongs to
 * the IdP.</p>
 */
public record AuthenticatedUser(
        String subject,
        String username,
        Set<String> authorities,
        boolean passwordChangeRequired
) implements SixpayPrincipal {

    private static final String ROLE_PREFIX = "ROLE_";

    /**
     * Compatibility constructor used by OIDC and existing callers.
     */
    public AuthenticatedUser(
            String subject,
            String username,
            Set<String> authorities
    ) {
        this(
                subject,
                username,
                authorities,
                false
        );
    }

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

    @Override
    public Set<String> roles() {
        return authorities.stream()
                .filter(
                        authority ->
                                authority.startsWith(
                                        ROLE_PREFIX
                                )
                )
                .map(
                        authority ->
                                authority.substring(
                                        ROLE_PREFIX.length()
                                )
                )
                .collect(
                        Collectors.toUnmodifiableSet()
                );
    }

    @Override
    public Set<String> permissions() {
        return authorities.stream()
                .filter(
                        authority ->
                                !authority.startsWith(
                                        ROLE_PREFIX
                                )
                )
                .collect(
                        Collectors.toUnmodifiableSet()
                );
    }

    @Override
    public boolean hasAuthority(
            String authority
    ) {
        String validatedAuthority =
                Preconditions.requireNonBlank(
                        authority,
                        "Authority must not be blank"
                );

        return authorities.contains(
                validatedAuthority
        );
    }

    public boolean hasRole(
            SixpayRole role
    ) {
        SixpayRole validatedRole =
                Preconditions.requireNonNull(
                        role,
                        "Role must not be null"
                );

        return hasAuthority(
                validatedRole.authority()
        );
    }
}
