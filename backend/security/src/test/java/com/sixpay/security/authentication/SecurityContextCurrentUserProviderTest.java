package com.sixpay.security.authentication;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority
        .SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource
        .authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityContextCurrentUserProviderTest {

    private final CurrentUserProvider provider =
            new SecurityContextCurrentUserProvider();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnEmptyWithoutAuthentication() {
        Optional<AuthenticatedUser> user =
                provider.currentUser();

        assertTrue(user.isEmpty());
    }

    @Test
    void shouldMapOidcJwtAuthenticationToCanonicalPrincipal() {
        Instant issuedAt =
                Instant.parse("2026-07-26T12:00:00Z");

        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("oidc-user-123")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(300))
                .claim(
                        "preferred_username",
                        "rodrigue"
                )
                .build();

        JwtAuthenticationToken authentication =
                new JwtAuthenticationToken(
                        jwt,
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_ADMIN"
                                ),
                                new SimpleGrantedAuthority(
                                        "SCOPE_payment.read"
                                )
                        ),
                        "rodrigue"
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        SixpayPrincipal principal =
                provider.requireCurrentUser();

        assertInstanceOf(AuthenticatedUser.class, principal);
        assertEquals(
                "oidc-user-123",
                principal.subject()
        );
        assertEquals(
                "rodrigue",
                principal.username()
        );
        assertEquals(
                Set.of("ADMIN"),
                principal.roles()
        );
        assertEquals(
                Set.of("SCOPE_payment.read"),
                principal.permissions()
        );
    }

    @Test
    void shouldMapLocalAuthenticationToSameCanonicalPrincipalContract() {
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        "rodrigue",
                        null,
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_ADMIN"
                                ),
                                new SimpleGrantedAuthority(
                                        "SCOPE_payment.read"
                                )
                        )
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        SixpayPrincipal principal =
                provider.requireCurrentUser();

        assertInstanceOf(AuthenticatedUser.class, principal);
        assertEquals(
                "rodrigue",
                principal.subject()
        );
        assertEquals(
                "rodrigue",
                principal.username()
        );
        assertEquals(
                Set.of("ADMIN"),
                principal.roles()
        );
        assertEquals(
                Set.of("SCOPE_payment.read"),
                principal.permissions()
        );
    }

    @Test
    void shouldExposeSameContractShapeForLocalAndOidcAuthentication() {
        UsernamePasswordAuthenticationToken localAuthentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        "local-user",
                        null,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_AUDITOR"),
                                new SimpleGrantedAuthority(
                                        "SCOPE_payment.read"
                                )
                        )
                );

        SecurityContextHolder.getContext()
                .setAuthentication(localAuthentication);

        SixpayPrincipal localPrincipal =
                provider.requireCurrentUser();

        SecurityContextHolder.clearContext();

        Instant issuedAt =
                Instant.parse("2026-07-26T12:00:00Z");

        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("external-subject")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(300))
                .claim("preferred_username", "oidc-user")
                .build();

        JwtAuthenticationToken oidcAuthentication =
                new JwtAuthenticationToken(
                        jwt,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_AUDITOR"),
                                new SimpleGrantedAuthority(
                                        "SCOPE_payment.read"
                                )
                        ),
                        "oidc-user"
                );

        SecurityContextHolder.getContext()
                .setAuthentication(oidcAuthentication);

        SixpayPrincipal oidcPrincipal =
                provider.requireCurrentUser();

        assertEquals(
                localPrincipal.roles(),
                oidcPrincipal.roles()
        );
        assertEquals(
                localPrincipal.permissions(),
                oidcPrincipal.permissions()
        );
        assertEquals(
                AuthenticatedUser.class,
                localPrincipal.getClass()
        );
        assertEquals(
                localPrincipal.getClass(),
                oidcPrincipal.getClass()
        );
    }
}
