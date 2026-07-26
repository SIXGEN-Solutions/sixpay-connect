package com.sixpay.security.authentication;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority
        .SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource
        .authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void shouldReturnAuthenticatedJwtUser() {
        Instant issuedAt =
                Instant.parse("2026-07-26T12:00:00Z");

        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("user-123")
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
                                )
                        ),
                        "rodrigue"
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        AuthenticatedUser currentUser =
                provider.requireCurrentUser();

        assertEquals(
                "user-123",
                currentUser.subject()
        );

        assertEquals(
                "rodrigue",
                currentUser.username()
        );

        assertTrue(
                currentUser.authorities()
                        .contains("ROLE_ADMIN")
        );
    }
}