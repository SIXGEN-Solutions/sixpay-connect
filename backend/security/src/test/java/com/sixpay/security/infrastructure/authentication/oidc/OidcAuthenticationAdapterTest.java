package com.sixpay.security.infrastructure.authentication.oidc;

import com.sixpay.security.application.service.SubjectExternalIdentityResolver;
import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.jwt.SixpayJwtAuthoritiesConverter;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class OidcAuthenticationAdapterTest {

    @Test
    void convertsTrustedOidcJwtToCanonicalSixpayPrincipal() {
        Instant issuedAt =
                Instant.parse("2026-08-11T01:00:00Z");

        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .issuer("https://test-idp.sixpay.local")
                .subject("external-subject-123")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(300))
                .claim(
                        "preferred_username",
                        "oidc.user@sixpay.test"
                )
                .claim(
                        "roles",
                        List.of("ADMIN")
                )
                .claim(
                        "scope",
                        "payment.read"
                )
                .build();

        OidcAuthenticationAdapter adapter =
                new OidcAuthenticationAdapter(
                        new SixpayJwtAuthoritiesConverter(),
                        new SubjectExternalIdentityResolver()
                );

        OidcAuthenticationToken authentication =
                (OidcAuthenticationToken)
                        adapter.convert(jwt);

        assertThat(authentication.isAuthenticated())
                .isTrue();

        assertThat(authentication.getPrincipal())
                .isInstanceOf(AuthenticatedUser.class);

        AuthenticatedUser principal =
                authentication.getPrincipal();

        assertThat(principal.subject())
                .isEqualTo("external-subject-123");

        assertThat(principal.username())
                .isEqualTo("oidc.user@sixpay.test");

        assertThat(principal.roles())
                .containsExactly("ADMIN");

        assertThat(principal.permissions())
                .containsExactly("SCOPE_payment.read");

        assertThat(authentication.getCredentials())
                .isEqualTo("");
    }

    @Test
    void fallsBackToEmailWhenPreferredUsernameIsAbsent() {
        Instant issuedAt =
                Instant.parse("2026-08-11T01:00:00Z");

        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .issuer("https://test-idp.sixpay.local")
                .subject("external-subject-456")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(300))
                .claim(
                        "email",
                        "fallback@sixpay.test"
                )
                .build();

        OidcAuthenticationAdapter adapter =
                new OidcAuthenticationAdapter(
                        new SixpayJwtAuthoritiesConverter(),
                        new SubjectExternalIdentityResolver()
                );

        OidcAuthenticationToken authentication =
                (OidcAuthenticationToken)
                        adapter.convert(jwt);

        assertThat(
                authentication
                        .getPrincipal()
                        .username()
        ).isEqualTo("fallback@sixpay.test");
    }
}
