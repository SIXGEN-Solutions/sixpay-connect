package com.sixpay.security.domain.authentication;

import com.sixpay.common.validation.Preconditions;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Canonical SIXPAY user account.
 *
 * <p>Business roles and permissions belong to the SIXPAY user, never to the
 * authentication identity or external Identity Provider.</p>
 */
public record SixpayUserAccount(
        UUID id,
        String username,
        String email,
        SixpayUserAccountStatus status,
        Set<String> roles,
        Set<String> permissions
) {

    private static final String ROLE_PREFIX = "ROLE_";

    public SixpayUserAccount {
        id = Preconditions.requireNonNull(
                id,
                "SIXPAY user id must not be null"
        );
        username = Preconditions.requireNonBlank(
                username,
                "SIXPAY username must not be blank"
        );
        status = Preconditions.requireNonNull(
                status,
                "SIXPAY user status must not be null"
        );
        email = email == null || email.isBlank()
                ? null
                : email.trim();

        roles = Set.copyOf(
                Preconditions.requireNonNull(
                        roles,
                        "SIXPAY roles must not be null"
                )
        );

        permissions = Set.copyOf(
                Preconditions.requireNonNull(
                        permissions,
                        "SIXPAY permissions must not be null"
                )
        );
    }

    public boolean active() {
        return status == SixpayUserAccountStatus.ACTIVE;
    }

    public String canonicalSubject() {
        return id.toString();
    }

    /**
     * Spring-compatible authority representation derived exclusively from
     * SIXPAY-owned authorization data.
     */
    public Set<String> authorities() {
        Set<String> authorities = new LinkedHashSet<>();

        roles.stream()
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .map(role -> role.startsWith(ROLE_PREFIX)
                        ? role
                        : ROLE_PREFIX + role)
                .forEach(authorities::add);

        permissions.stream()
                .map(String::trim)
                .filter(permission -> !permission.isBlank())
                .forEach(authorities::add);

        return Set.copyOf(authorities);
    }
}
