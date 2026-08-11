package com.sixpay.security.jwt;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SixpayJwtAuthoritiesConverterTest {

    private final SixpayJwtAuthoritiesConverter converter =
            new SixpayJwtAuthoritiesConverter();

    @Test
    void shouldNotConvertProviderScopesOrRolesIntoBusinessAuthorities() {
        Instant issuedAt =
                Instant.parse("2026-07-26T12:00:00Z");

        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("user-123")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(300))
                .claim("scope", "payment.read payment.write")
                .claim("roles", List.of("ADMIN", "OPS"))
                .build();

        assertTrue(converter.convert(jwt).isEmpty());
    }
}
