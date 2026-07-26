package com.sixpay.security.jwt;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SixpayJwtAuthoritiesConverterTest {

    private final SixpayJwtAuthoritiesConverter converter =
            new SixpayJwtAuthoritiesConverter();

    @Test
    void shouldConvertScopesAndRoles() {
        Jwt jwt = createJwt(
                "payment.read payment.write",
                List.of("admin", "ops")
        );

        Collection<GrantedAuthority> authorities =
                converter.convert(jwt);

        Set<String> names = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        assertEquals(4, names.size());

        assertTrue(names.contains("SCOPE_payment.read"));
        assertTrue(names.contains("SCOPE_payment.write"));
        assertTrue(names.contains("ROLE_ADMIN"));
        assertTrue(names.contains("ROLE_OPS"));
    }

    @Test
    void shouldAvoidDuplicatingRolePrefix() {
        Jwt jwt = createJwt(
                "",
                List.of("ROLE_ADMIN")
        );

        Collection<GrantedAuthority> authorities =
                converter.convert(jwt);

        Set<String> names = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        assertEquals(Set.of("ROLE_ADMIN"), names);
    }

    private Jwt createJwt(
            String scope,
            List<String> roles
    ) {
        Instant issuedAt =
                Instant.parse("2026-07-26T12:00:00Z");

        return Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("user-123")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(300))
                .claim("scope", scope)
                .claim("roles", roles)
                .build();
    }
}