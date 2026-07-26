package com.sixpay.security.authentication;

import com.sixpay.security.authorization.SixpayRole;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticatedUserTest {

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