package com.sixpay.security.configuration;

import com.sixpay.security.application.port.in.AuthenticateLocalUserUseCase;
import com.sixpay.security.application.port.in.ChangeLocalPasswordUseCase;
import com.sixpay.security.application.port.out.AuthenticationAuditPort;
import com.sixpay.security.application.port.out.ExternalIdentityResolver;
import com.sixpay.security.application.port.out.SecurityAuditPort;
import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.infrastructure.authentication.persistence.LocalAuthenticationUserSpringDataRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DA-11.4 — Hybrid coexistence integration/security coverage.
 *
 * <p>This test runs SIXPAY with LOCAL and OIDC capabilities enabled at the
 * same time. Authentication persistence and external identity resolution are
 * boundary doubles; the Spring Security filter chain, LOCAL controller,
 * OIDC resource-server integration and shared SIXPAY session convergence are
 * real.</p>
 */
@SpringBootTest(
        classes = HybridAuthenticationIT.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "sixpay.security.authentication.local.enabled=true",
                "sixpay.security.authentication.oidc.enabled=true"
        }
)
@AutoConfigureMockMvc
class HybridAuthenticationIT {

    private static final UUID USER_ID =
            UUID.fromString(
                    "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
            );

    private static final String TOKEN =
            "hybrid-provider-token";

    private static final String ISSUER =
            "https://test-idp.sixpay.local";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private ExternalIdentityResolver externalIdentityResolver;

    @MockitoBean
    private SecurityAuditPort securityAuditPort;

    /*
     * LOCAL persistence is outside the purpose of DA-11.4. The use case is a
     * boundary double while the real LOCAL HTTP/session integration remains
     * active.
     */
    @MockitoBean
    private AuthenticateLocalUserUseCase authenticateLocalUserUseCase;

    /*
     * These collaborators satisfy LOCAL auto-configuration without bringing
     * database infrastructure into this focused coexistence test.
     */
    @MockitoBean
    private AuthenticationAuditPort authenticationAuditPort;

    @MockitoBean
    private ChangeLocalPasswordUseCase changeLocalPasswordUseCase;

    @MockitoBean
    private LocalAuthenticationUserSpringDataRepository
            localAuthenticationUserSpringDataRepository;

    @Test
    void localAuthenticationRemainsAvailableWhenOidcIsAlsoEnabled()
            throws Exception {

        when(
                authenticateLocalUserUseCase.authenticate(
                        any()
                )
        )
                .thenReturn(
                        localManager()
                );

        var loginResult =
                mockMvc.perform(
                                post(
                                        "/api/v1/auth/login"
                                )
                                        .contentType(
                                                MediaType.APPLICATION_JSON
                                        )
                                        .content(
                                                """
                                                {
                                                  "username": "manager",
                                                  "password": "manager-dev-2027"
                                                }
                                                """
                                        )
                        )
                        .andExpect(
                                status().isOk()
                        )
                        .andExpect(
                                jsonPath("$.authenticated")
                                        .value(true)
                        )
                        .andExpect(
                                jsonPath("$.subject")
                                        .value(
                                                USER_ID.toString()
                                        )
                        )
                        .andExpect(
                                jsonPath("$.username")
                                        .value("manager")
                        )
                        .andExpect(
                                jsonPath("$.authenticationMethod")
                                        .value("LOCAL")
                        )
                        .andExpect(
                                jsonPath("$.passwordChangeRequired")
                                        .value(false)
                        )
                        .andReturn();

        MockHttpSession localSession =
                (MockHttpSession)
                        loginResult
                                .getRequest()
                                .getSession(false);

        mockMvc.perform(
                        get(
                                "/api/v1/auth/me"
                        )
                                .session(
                                        localSession
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.authenticationMethod")
                                .value("LOCAL")
                )
                .andExpect(
                        jsonPath("$.username")
                                .value("manager")
                );

        verify(
                authenticateLocalUserUseCase
        )
                .authenticate(
                        any()
                );
    }

    @Test
    void oidcBearerAuthenticationRemainsAvailableWhenLocalIsAlsoEnabled()
            throws Exception {

        stubOidcAuthentication(
                oidcAdmin(false)
        );

        mockMvc.perform(
                        get(
                                "/api/v1/auth/me"
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + TOKEN
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.authenticated")
                                .value(true)
                )
                .andExpect(
                        jsonPath("$.subject")
                                .value(
                                        USER_ID.toString()
                                )
                )
                .andExpect(
                        jsonPath("$.username")
                                .value("oidc-admin")
                )
                .andExpect(
                        jsonPath("$.authenticationMethod")
                                .value("OIDC")
                )
                .andExpect(
                        jsonPath("$.passwordChangeRequired")
                                .value(false)
                );

        verify(
                externalIdentityResolver
        )
                .resolve(
                        any()
                );
    }

    @Test
    void localAndOidcConvergeToSameCanonicalSixpayAuthorizationModel()
            throws Exception {

        AuthenticatedUser canonicalUser =
                new AuthenticatedUser(
                        USER_ID.toString(),
                        "manager",
                        Set.of(
                                "ROLE_MANAGER",
                                "SCOPE_payment.read",
                                "SCOPE_payment.write"
                        ),
                        false
                );

        when(
                authenticateLocalUserUseCase.authenticate(
                        any()
                )
        )
                .thenReturn(
                        canonicalUser
                );

        var localResult =
                mockMvc.perform(
                                post(
                                        "/api/v1/auth/login"
                                )
                                        .contentType(
                                                MediaType.APPLICATION_JSON
                                        )
                                        .content(
                                                """
                                                {
                                                  "username": "manager",
                                                  "password": "manager-dev-2027"
                                                }
                                                """
                                        )
                        )
                        .andExpect(
                                status().isOk()
                        )
                        .andExpect(
                                jsonPath("$.roles[0]")
                                        .value("MANAGER")
                        )
                        .andExpect(
                                jsonPath("$.permissions.length()")
                                        .value(2)
                        )
                        .andReturn();

        MockHttpSession localSession =
                (MockHttpSession)
                        localResult
                                .getRequest()
                                .getSession(false);

        mockMvc.perform(
                        get(
                                "/api/v1/auth/me"
                        )
                                .session(
                                        localSession
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.roles[0]")
                                .value("MANAGER")
                )
                .andExpect(
                        jsonPath("$.permissions.length()")
                                .value(2)
                );

        stubOidcAuthentication(
                canonicalUser
        );

        /*
         * The provider JWT may identify the user differently, but the
         * resulting roles/permissions are the canonical SIXPAY ones returned
         * by ExternalIdentityResolver.
         */
        mockMvc.perform(
                        get(
                                "/api/v1/auth/me"
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + TOKEN
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.subject")
                                .value(
                                        USER_ID.toString()
                                )
                )
                .andExpect(
                        jsonPath("$.username")
                                .value("manager")
                )
                .andExpect(
                        jsonPath("$.authenticationMethod")
                                .value("OIDC")
                )
                .andExpect(
                        jsonPath("$.roles[0]")
                                .value("MANAGER")
                )
                .andExpect(
                        jsonPath("$.permissions.length()")
                                .value(2)
                );
    }

    @Test
    void oidcNeverExposesLocalPasswordChangeRequirement()
            throws Exception {

        /*
         * Even if the canonical account carries a LOCAL password lifecycle
         * flag, an OIDC authentication must report false because the password
         * is owned by the external IdP.
         */
        stubOidcAuthentication(
                oidcAdmin(true)
        );

        mockMvc.perform(
                        get(
                                "/api/v1/auth/me"
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + TOKEN
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.authenticationMethod")
                                .value("OIDC")
                )
                .andExpect(
                        jsonPath("$.passwordChangeRequired")
                                .value(false)
                );
    }

    @Test
    void oidcBearerCanBePromotedToBackendSessionWithoutDisablingLocalCapability()
            throws Exception {

        stubOidcAuthentication(
                oidcAdmin(false)
        );

        var oidcSessionResult =
                mockMvc.perform(
                                post(
                                        "/api/v1/auth/session/oidc"
                                )
                                        .header(
                                                "Authorization",
                                                "Bearer " + TOKEN
                                        )
                        )
                        .andExpect(
                                status().isOk()
                        )
                        .andExpect(
                                jsonPath("$.authenticationMethod")
                                        .value("OIDC")
                        )
                        .andReturn();

        MockHttpSession oidcSession =
                (MockHttpSession)
                        oidcSessionResult
                                .getRequest()
                                .getSession(false);

        mockMvc.perform(
                        get(
                                "/api/v1/auth/me"
                        )
                                .session(
                                        oidcSession
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.authenticationMethod")
                                .value("OIDC")
                )
                .andExpect(
                        jsonPath("$.username")
                                .value("oidc-admin")
                );

        /*
         * Establishing an OIDC session does not remove or disable the LOCAL
         * authentication capability. A new independent LOCAL login can still
         * be created.
         */
        when(
                authenticateLocalUserUseCase.authenticate(
                        any()
                )
        )
                .thenReturn(
                        localManager()
                );

        mockMvc.perform(
                        post(
                                "/api/v1/auth/login"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "username": "manager",
                                          "password": "manager-dev-2027"
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.authenticationMethod")
                                .value("LOCAL")
                );
    }

    private void stubOidcAuthentication(
            AuthenticatedUser user
    ) {
        when(
                jwtDecoder.decode(
                        TOKEN
                )
        )
                .thenReturn(
                        providerJwt()
                );

        when(
                externalIdentityResolver.resolve(
                        any()
                )
        )
                .thenReturn(
                        user
                );
    }

    private static AuthenticatedUser localManager() {
        return new AuthenticatedUser(
                USER_ID.toString(),
                "manager",
                Set.of(
                        "ROLE_MANAGER",
                        "SCOPE_payment.read"
                ),
                false
        );
    }

    private static AuthenticatedUser oidcAdmin(
            boolean passwordChangeRequired
    ) {
        return new AuthenticatedUser(
                USER_ID.toString(),
                "oidc-admin",
                Set.of(
                        "ROLE_ADMIN",
                        "SCOPE_payment.read"
                ),
                passwordChangeRequired
        );
    }

    private static Jwt providerJwt() {
        Instant issuedAt =
                Instant.parse(
                        "2026-08-16T04:00:00Z"
                );

        return Jwt.withTokenValue(
                        TOKEN
                )
                .header(
                        "alg",
                        "RS256"
                )
                .issuer(
                        ISSUER
                )
                .subject(
                        "provider-user-123"
                )
                .issuedAt(
                        issuedAt
                )
                .expiresAt(
                        issuedAt.plusSeconds(
                                300
                        )
                )
                .claim(
                        "preferred_username",
                        "provider.user@sixpay.test"
                )
                /*
                 * Provider authorization is deliberately not consumed by
                 * SIXPAY. Authorization comes from ExternalIdentityResolver.
                 */
                .claim(
                        "roles",
                        Set.of(
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
    }
}
