package com.sixpay.security.authentication;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.security.Principal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class AuthenticatedUserPrincipalTest {

    @Test
    void exposesCanonicalSubjectAsSpringAuthenticationName() {
        AuthenticatedUser user = new AuthenticatedUser(
                "11111111-1111-4111-8111-111111111111",
                "fullstack-admin@sixpay.test",
                Set.of(
                        "ROLE_ADMIN",
                        "SCOPE_payment.read",
                        "SCOPE_payment.audit.read",
                        "SCOPE_customer.read"
                ),
                false
        );

        Principal principal = assertInstanceOf(Principal.class, user);

        var authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        user,
                        null,
                        user.authorities().stream()
                                .map(SimpleGrantedAuthority::new)
                                .toList()
                );

        assertEquals(user.subject(), principal.getName());
        assertEquals(user.subject(), authentication.getName());
    }
}
