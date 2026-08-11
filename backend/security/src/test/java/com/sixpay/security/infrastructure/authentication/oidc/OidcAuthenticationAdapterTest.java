package com.sixpay.security.infrastructure.authentication.oidc;

import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.jwt.SixpayJwtAuthoritiesConverter;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OidcAuthenticationAdapterTest {

    @Test
    void convertsTrustedOidcJwtToLinkedCanonicalPrincipal() {
        UUID userId = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
        Instant issuedAt = Instant.parse("2026-08-11T01:00:00Z");

        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .issuer("https://test-idp.sixpay.local")
                .subject("external-subject-123")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(300))
                .claim("preferred_username", "oidc.user@sixpay.test")
                .claim("roles", List.of("ADMIN"))
                .claim("scope", "payment.read")
                .build();

        OidcAuthenticationAdapter adapter =
                new OidcAuthenticationAdapter(
                        new SixpayJwtAuthoritiesConverter(),
                        (identity, authorities) ->
                                new AuthenticatedUser(
                                        userId.toString(),
                                        "rodrigue",
                                        authorities
                                )
                );

        OidcAuthenticationToken authentication =
                (OidcAuthenticationToken) adapter.convert(jwt);

        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getPrincipal().subject())
                .isEqualTo(userId.toString());
        assertThat(authentication.getPrincipal().username())
                .isEqualTo("rodrigue");
        assertThat(authentication.getPrincipal().roles())
                .containsExactly("ADMIN");
        assertThat(authentication.getPrincipal().permissions())
                .containsExactly("SCOPE_payment.read");
    }

    @Test
    void convertsUnlinkedIdentityFailureToOauthAuthenticationFailure() {
        Instant issuedAt = Instant.parse("2026-08-11T01:00:00Z");

        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .issuer("https://test-idp.sixpay.local")
                .subject("unknown-subject")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(300))
                .build();

        OidcAuthenticationAdapter adapter =
                new OidcAuthenticationAdapter(
                        new SixpayJwtAuthoritiesConverter(),
                        (identity, authorities) -> {
                            throw new com.sixpay.security.application.exception.ExternalIdentityNotLinkedException();
                        }
                );

        assertThatThrownBy(() -> adapter.convert(jwt))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }
}
