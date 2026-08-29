package com.sixpay.security.configuration;

import com.sixpay.security.application.exception.ExternalIdentityNotLinkedException;
import com.sixpay.security.application.exception.SixpayUserDisabledException;
import com.sixpay.security.application.port.output.ExternalIdentityResolver;
import com.sixpay.security.application.port.output.SecurityAuditPort;
import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.authentication.CurrentUserProvider;
import com.sixpay.security.domain.administration.SecurityAuditEventType;
import com.sixpay.security.domain.authentication.ExternalIdentity;
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
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes =
                OidcAuthenticationProviderIT.TestApplication.class,
        webEnvironment =
                SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "sixpay.security.authentication.local.enabled=false",
                "sixpay.security.authentication.oidc.enabled=true"
        }
)
@AutoConfigureMockMvc
class OidcAuthenticationProviderIT {

    private static final UUID USER_ID =
            UUID.fromString(
                    "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
            );

    private static final String ISSUER =
            "https://test-idp.sixpay.local";

    private static final String PROVIDER_SUBJECT =
            "provider-user-123";

    private static final String TOKEN =
            "test-provider-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private ExternalIdentityResolver
            externalIdentityResolver;

    @MockitoBean
    private SecurityAuditPort securityAuditPort;

    @Test
    void authenticatesBearerUsingOnlySixpayOwnedAuthorization()
            throws Exception {

        when(
                jwtDecoder.decode(TOKEN)
        )
                .thenReturn(
                        providerJwt(
                                TOKEN,
                                ISSUER,
                                PROVIDER_SUBJECT
                        )
                );

        when(
                externalIdentityResolver.resolve(
                        any(ExternalIdentity.class)
                )
        )
                .thenReturn(
                        canonicalAdmin()
                );

        mockMvc.perform(
                        get("/identity")
                                .header(
                                        "Authorization",
                                        "Bearer " + TOKEN
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        content().string(
                                USER_ID
                                        + "|rodrigue"
                                        + "|ADMIN"
                                        + "|SCOPE_payment.read"
                                        + "|false"
                        )
                );

        verify(
                externalIdentityResolver
        )
                .resolve(
                        argThat(identity ->
                                ISSUER.equals(
                                        identity.issuer()
                                )
                                        && PROVIDER_SUBJECT.equals(
                                        identity.subject()
                                )
                                        && "provider.user@sixpay.test"
                                        .equals(
                                                identity.username()
                                        )
                        )
                );

        verify(
                securityAuditPort
        )
                .record(
                        argThat(event ->
                                event.eventType()
                                        == SecurityAuditEventType.OIDC_LOGIN_SUCCESS
                                        && USER_ID.toString()
                                        .equals(
                                                event.actorSubject()
                                        )
                                        && USER_ID.equals(
                                                event.targetUserId()
                                        )
                                        && "rodrigue".equals(
                                                event.username()
                                        )
                                        && ISSUER.equals(
                                                event.provider()
                                        )
                        )
                );
    }

    @Test
    void rejectsUnlinkedExternalIdentityAndAuditsBearerFailure()
            throws Exception {

        when(
                jwtDecoder.decode(TOKEN)
        )
                .thenReturn(
                        providerJwt(
                                TOKEN,
                                ISSUER,
                                PROVIDER_SUBJECT
                        )
                );

        when(
                externalIdentityResolver.resolve(
                        any(ExternalIdentity.class)
                )
        )
                .thenThrow(
                        new ExternalIdentityNotLinkedException()
                );

        mockMvc.perform(
                        get("/identity")
                                .header(
                                        "Authorization",
                                        "Bearer " + TOKEN
                                )
                )
                .andExpect(
                        status().isUnauthorized()
                )
                .andExpect(
                        header().string(
                                "WWW-Authenticate",
                                org.hamcrest.Matchers
                                        .containsString(
                                                "Bearer"
                                        )
                        )
                );

        verifyOidcFailureAudit();

        verify(
                securityAuditPort,
                never()
        )
                .record(
                        argThat(event ->
                                event.eventType()
                                        == SecurityAuditEventType.OIDC_LOGIN_SUCCESS
                        )
                );
    }

    @Test
    void rejectsDisabledCanonicalSixpayUserAndAuditsBearerFailure()
            throws Exception {

        when(
                jwtDecoder.decode(TOKEN)
        )
                .thenReturn(
                        providerJwt(
                                TOKEN,
                                ISSUER,
                                PROVIDER_SUBJECT
                        )
                );

        when(
                externalIdentityResolver.resolve(
                        any(ExternalIdentity.class)
                )
        )
                .thenThrow(
                        new SixpayUserDisabledException()
                );

        mockMvc.perform(
                        get("/identity")
                                .header(
                                        "Authorization",
                                        "Bearer " + TOKEN
                                )
                )
                .andExpect(
                        status().isUnauthorized()
                );

        verifyOidcFailureAudit();

        verify(
                securityAuditPort,
                never()
        )
                .record(
                        argThat(event ->
                                event.eventType()
                                        == SecurityAuditEventType.OIDC_LOGIN_SUCCESS
                        )
                );
    }

    @Test
    void rejectsInvalidBearerBeforeIdentityResolution()
            throws Exception {

        when(
                jwtDecoder.decode(TOKEN)
        )
                .thenThrow(
                        new BadJwtException(
                                "JWT signature validation failed"
                        )
                );

        mockMvc.perform(
                        get("/identity")
                                .header(
                                        "Authorization",
                                        "Bearer " + TOKEN
                                )
                )
                .andExpect(
                        status().isUnauthorized()
                )
                .andExpect(
                        header().string(
                                "WWW-Authenticate",
                                org.hamcrest.Matchers
                                        .containsString(
                                                "invalid_token"
                                        )
                        )
                );

        verifyNoInteractions(
                externalIdentityResolver
        );

        verifyOidcFailureAudit();
    }

    @Test
    void rejectsBearerMissingRequiredIssuerClaimBeforeIdentityResolution()
            throws Exception {

        Jwt jwt =
                Jwt.withTokenValue(TOKEN)
                        .header(
                                "alg",
                                "RS256"
                        )
                        .subject(
                                PROVIDER_SUBJECT
                        )
                        .issuedAt(
                                Instant.parse(
                                        "2026-08-16T04:00:00Z"
                                )
                        )
                        .expiresAt(
                                Instant.parse(
                                        "2026-08-16T04:05:00Z"
                                )
                        )
                        .claim(
                                "preferred_username",
                                "provider.user@sixpay.test"
                        )
                        .build();

        when(
                jwtDecoder.decode(TOKEN)
        )
                .thenReturn(jwt);

        mockMvc.perform(
                        get("/identity")
                                .header(
                                        "Authorization",
                                        "Bearer " + TOKEN
                                )
                )
                .andExpect(
                        status().isUnauthorized()
                );

        verifyNoInteractions(
                externalIdentityResolver
        );

        verifyOidcFailureAudit();
    }

    @Test
    void anonymousRequestRemainsUnauthenticatedWithoutOidcFailureAudit()
            throws Exception {

        mockMvc.perform(
                        get("/identity")
                )
                .andExpect(
                        status().isUnauthorized()
                );

        verify(
                securityAuditPort,
                never()
        )
                .record(
                        argThat(event ->
                                event.eventType()
                                        == SecurityAuditEventType.OIDC_LOGIN_FAILURE
                        )
                );
    }

    private void verifyOidcFailureAudit() {
        verify(
                securityAuditPort
        )
                .record(
                        argThat(event ->
                                event.eventType()
                                        == SecurityAuditEventType.OIDC_LOGIN_FAILURE
                                        && event.actorSubject() == null
                                        && event.targetUserId() == null
                                        && event.username() == null
                                        && event.provider() == null
                                        && "bearer-authentication-failed"
                                        .equals(
                                                event.detail()
                                        )
                        )
                );
    }

    private static AuthenticatedUser canonicalAdmin() {
        return new AuthenticatedUser(
                USER_ID.toString(),
                "rodrigue",
                Set.of(
                        "ROLE_ADMIN",
                        "SCOPE_payment.read"
                )
        );
    }

    private static Jwt providerJwt(
            String tokenValue,
            String issuer,
            String subject
    ) {
        Instant issuedAt =
                Instant.parse(
                        "2026-08-16T04:00:00Z"
                );

        return Jwt.withTokenValue(
                        tokenValue
                )
                .header(
                        "alg",
                        "RS256"
                )
                .issuer(issuer)
                .subject(subject)
                .issuedAt(issuedAt)
                .expiresAt(
                        issuedAt.plusSeconds(300)
                )
                .claim(
                        "preferred_username",
                        "provider.user@sixpay.test"
                )
                .claim(
                        "roles",
                        List.of(
                                "PROVIDER_SUPER_ADMIN"
                        )
                )
                .claim(
                        "scope",
                        "provider.everything"
                )
                .build();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(
            exclude = {
                    DataSourceAutoConfiguration.class,
                    HibernateJpaAutoConfiguration.class,
                    DataJpaRepositoriesAutoConfiguration.class
            }
    )
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

        private final CurrentUserProvider
                currentUserProvider;

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
                            + "|"
                            + user.passwordChangeRequired()
            );
        }
    }
}
