package com.sixpay.security.infrastructure.authentication.oidc;

import com.sixpay.security.authentication.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OidcAuthenticationAdapterTest {

    @Test
    void ignoresProviderRolesAndScopesAndUsesResolvedSixpayAuthorities() {
        UUID userId =
                UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
        Instant issuedAt =
                Instant.parse("2026-08-11T01:00:00Z");

        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .issuer("https://test-idp.sixpay.local")
                .subject("external-subject-123")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(300))
                .claim("preferred_username", "provider.user@sixpay.test")
                .claim("roles", List.of("SUPER_ADMIN_FROM_IDP"))
                .claim("scope", "dangerous.provider.scope")
                .build();

        OidcAuthenticationAdapter adapter =
                new OidcAuthenticationAdapter(
                        identity ->
                                new AuthenticatedUser(
                                        userId.toString(),
                                        "rodrigue",
                                        Set.of(
                                                "ROLE_AUDITOR",
                                                "SCOPE_payment.read"
                                        )
                                )
                );

        OidcAuthenticationToken authentication =
                (OidcAuthenticationToken)
                        adapter.convert(jwt);

        assertThat(authentication.getPrincipal().roles())
                .containsExactly("AUDITOR");
        assertThat(authentication.getPrincipal().permissions())
                .containsExactly("SCOPE_payment.read");

        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder(
                        "ROLE_AUDITOR",
                        "SCOPE_payment.read"
                )
                .doesNotContain(
                        "ROLE_SUPER_ADMIN_FROM_IDP",
                        "SCOPE_dangerous.provider.scope"
                );
    }
}
