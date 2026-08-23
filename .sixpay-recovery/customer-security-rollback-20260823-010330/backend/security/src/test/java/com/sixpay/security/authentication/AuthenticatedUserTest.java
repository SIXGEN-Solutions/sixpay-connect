package com.sixpay.security.authentication;

import com.sixpay.security.authorization.SixpayRole;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticatedUserTest {

    @Test
    void shouldImplementCanonicalSixpayPrincipalContract() {
        AuthenticatedUser user = new AuthenticatedUser(
                "user-123",
                "rodrigue",
                Set.of(
                        "ROLE_ADMIN",
                        "SCOPE_payment.read"
                )
        );

        SixpayPrincipal principal =
                assertInstanceOf(SixpayPrincipal.class, user);

        assertEquals("user-123", principal.subject());
        assertEquals("rodrigue", principal.username());
        assertEquals(Set.of("ADMIN"), principal.roles());
        assertEquals(
                Set.of("SCOPE_payment.read"),
                principal.permissions()
        );
    }

    @Test
    void shouldRecognizeGrantedRole() {
        AuthenticatedUser user = new AuthenticatedUser(
                "user-123",
                "rodrigue",
                Set.of(
                        "ROLE_ADMIN",
                        "SCOPE_payment.read"
                )
        );

        assertTrue(user.hasRole(SixpayRole.ADMIN));
        assertFalse(user.hasRole(SixpayRole.AUDITOR));
    }

    @Test
    void shouldExposeRolesWithoutSpringRolePrefix() {
        AuthenticatedUser user = new AuthenticatedUser(
                "user-123",
                "rodrigue",
                Set.of(
                        "ROLE_ADMIN",
                        "ROLE_AUDITOR",
                        "SCOPE_payment.read"
                )
        );

        assertEquals(
                Set.of("ADMIN", "AUDITOR"),
                user.roles()
        );
    }

    @Test
    void shouldExposeNonRoleAuthoritiesAsPermissions() {
        AuthenticatedUser user = new AuthenticatedUser(
                "user-123",
                "rodrigue",
                Set.of(
                        "ROLE_ADMIN",
                        "SCOPE_payment.read",
                        "payment.export"
                )
        );

        assertEquals(
                Set.of(
                        "SCOPE_payment.read",
                        "payment.export"
                ),
                user.permissions()
        );
    }

    @Test
    void shouldRecognizeGrantedAuthority() {
        AuthenticatedUser user = new AuthenticatedUser(
                "user-123",
                "rodrigue",
                Set.of("SCOPE_payment.read")
        );

        assertTrue(
                user.hasAuthority("SCOPE_payment.read")
        );
    }

    @Test
    void shouldDefensivelyCopyAuthorities() {
        Set<String> authorities = new HashSet<>();
        authorities.add("ROLE_ADMIN");

        AuthenticatedUser user = new AuthenticatedUser(
                "user-123",
                "rodrigue",
                authorities
        );

        authorities.add("ROLE_AUDITOR");

        assertFalse(
                user.authorities().contains("ROLE_AUDITOR")
        );
    }

    @Test
    void shouldExposeImmutableDerivedRoleSet() {
        AuthenticatedUser user = new AuthenticatedUser(
                "user-123",
                "rodrigue",
                Set.of("ROLE_ADMIN")
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> user.roles().add("AUDITOR")
        );
    }

    @Test
    void shouldExposeImmutableDerivedPermissionSet() {
        AuthenticatedUser user = new AuthenticatedUser(
                "user-123",
                "rodrigue",
                Set.of("SCOPE_payment.read")
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> user.permissions().add("payment.export")
        );
    }

    @Test
    void shouldRejectBlankSubject() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AuthenticatedUser(
                        " ",
                        "rodrigue",
                        Set.of()
                )
        );
    }
}
