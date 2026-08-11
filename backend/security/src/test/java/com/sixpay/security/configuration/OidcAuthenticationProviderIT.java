package com.sixpay.security.configuration;

import com.sixpay.security.authentication.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = OidcAuthenticationProviderIT.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "sixpay.security.authentication.local.enabled=false",
                "sixpay.security.authentication.oidc.enabled=true"
        }
)
@AutoConfigureMockMvc
class OidcAuthenticationProviderIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void authenticatesBearerTokenThroughProviderNeutralOidcAdapter()
            throws Exception {

        Instant issuedAt =
                Instant.parse("2026-08-11T01:00:00Z");

        Jwt jwt = Jwt.withTokenValue("test-provider-token")
                .header("alg", "RS256")
                .issuer("https://test-idp.sixpay.local")
                .subject("provider-user-123")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(300))
                .claim(
                        "preferred_username",
                        "provider.user@sixpay.test"
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

        when(
                jwtDecoder.decode(
                        "test-provider-token"
                )
        ).thenReturn(jwt);

        mockMvc.perform(
                        get("/identity")
                                .header(
                                        "Authorization",
                                        "Bearer test-provider-token"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        content().string(
                                "provider-user-123"
                                        + "|provider.user@sixpay.test"
                                        + "|ADMIN"
                                        + "|SCOPE_payment.read"
                        )
                );
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            DataJpaRepositoriesAutoConfiguration.class
    })
    static class TestApplication {

        @Bean
        IdentityController identityController(
                CurrentUserProvider currentUserProvider
        ) {
            return new IdentityController(
                    currentUserProvider
            );
        }
    }

    @RestController
    static class IdentityController {

        private final CurrentUserProvider currentUserProvider;

        IdentityController(
                CurrentUserProvider currentUserProvider
        ) {
            this.currentUserProvider =
                    currentUserProvider;
        }

        @GetMapping("/identity")
        ResponseEntity<String> identity() {
            var user =
                    currentUserProvider
                            .requireCurrentUser();

            return ResponseEntity.ok(
                    user.subject()
                            + "|"
                            + user.username()
                            + "|"
                            + String.join(
                                    ",",
                                    user.roles()
                            )
                            + "|"
                            + String.join(
                                    ",",
                                    user.permissions()
                            )
            );
        }
    }
}
