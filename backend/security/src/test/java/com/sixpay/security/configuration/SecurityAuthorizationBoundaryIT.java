package com.sixpay.security.configuration;

import com.sixpay.security.application.port.output.ExternalIdentityResolver;
import com.sixpay.security.application.port.output.SecurityAuditPort;
import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.domain.authentication.ExternalIdentity;
import jakarta.servlet.http.Cookie;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = SecurityAuthorizationBoundaryIT.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "sixpay.security.authentication.local.enabled=false",
                "sixpay.security.authentication.oidc.enabled=true"
        }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAuthorizationBoundaryIT {

    private static final UUID USER_ID =
            UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");

    private static final String TOKEN =
            "authorization-boundary-token";

    private static final String RAW_XSRF_TOKEN =
            "a4d48244-3a22-405c-8e90-85af19ee5fc7";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private ExternalIdentityResolver externalIdentityResolver;

    @MockitoBean
    private SecurityAuditPort securityAuditPort;

    @Test
    void anonymousRequestIsRejectedAtAuthenticationBoundary()
            throws Exception {

        mockMvc.perform(
                        get("/test-security/admin")
                )
                .andExpect(
                        status().isUnauthorized()
                );
    }

    @Test
    void authenticatedUserWithoutRequiredRoleIsForbidden()
            throws Exception {

        mockMvc.perform(
                        get("/test-security/admin")
                                .with(
                                        user("auditor")
                                                .roles("AUDITOR")
                                )
                )
                .andExpect(
                        status().isForbidden()
                );
    }

    @Test
    void authenticatedUserWithRequiredRoleIsAuthorized()
            throws Exception {

        mockMvc.perform(
                        get("/test-security/admin")
                                .with(
                                        user("admin")
                                                .roles("ADMIN")
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        content().string("ADMIN")
                );
    }

    @Test
    void authenticatedUserWithoutRequiredPermissionIsForbidden()
            throws Exception {

        mockMvc.perform(
                        get("/test-security/payment-read")
                                .with(
                                        user("manager")
                                                .roles("MANAGER")
                                )
                )
                .andExpect(
                        status().isForbidden()
                );
    }

    @Test
    void authenticatedUserWithRequiredPermissionIsAuthorized()
            throws Exception {

        mockMvc.perform(
                        get("/test-security/payment-read")
                                .with(
                                        user("manager")
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "SCOPE_payment.read"
                                                        )
                                                )
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        content().string("PAYMENT_READ")
                );
    }

    @Test
    void sessionMutationWithoutCsrfTokenIsForbidden()
            throws Exception {

        mockMvc.perform(
                        post("/test-security/admin-mutation")
                                .with(
                                        user("admin")
                                                .roles("ADMIN")
                                )
                )
                .andExpect(
                        status().isForbidden()
                );
    }

    @Test
    void sessionMutationWithMatchingAngularXsrfCookieAndHeaderIsAuthorized()
            throws Exception {

        mockMvc.perform(
                        post("/test-security/admin-mutation")
                                .with(
                                        user("admin")
                                                .roles("ADMIN")
                                )
                                .cookie(
                                        new Cookie(
                                                "XSRF-TOKEN",
                                                RAW_XSRF_TOKEN
                                        )
                                )
                                .header(
                                        "X-XSRF-TOKEN",
                                        RAW_XSRF_TOKEN
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        content().string("MUTATED")
                );
    }

    @Test
    void sessionMutationWithMismatchedAngularXsrfTokenIsForbidden()
            throws Exception {

        mockMvc.perform(
                        post("/test-security/admin-mutation")
                                .with(
                                        user("admin")
                                                .roles("ADMIN")
                                )
                                .cookie(
                                        new Cookie(
                                                "XSRF-TOKEN",
                                                RAW_XSRF_TOKEN
                                        )
                                )
                                .header(
                                        "X-XSRF-TOKEN",
                                        "different-token"
                                )
                )
                .andExpect(
                        status().isForbidden()
                );
    }

    @Test
    void bearerMutationDoesNotRequireSessionCsrfToken()
            throws Exception {

        stubOidc(
                new AuthenticatedUser(
                        USER_ID.toString(),
                        "oidc-admin",
                        Set.of("ROLE_ADMIN"),
                        false
                )
        );

        mockMvc.perform(
                        post("/test-security/admin-mutation")
                                .header(
                                        "Authorization",
                                        "Bearer " + TOKEN
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        content().string("MUTATED")
                );

        verify(externalIdentityResolver)
                .resolve(any(ExternalIdentity.class));
    }

    @Test
    void providerRoleCannotCrossSixpayAuthorizationBoundary()
            throws Exception {

        stubOidc(
                new AuthenticatedUser(
                        USER_ID.toString(),
                        "canonical-auditor",
                        Set.of("ROLE_AUDITOR"),
                        false
                )
        );

        mockMvc.perform(
                        get("/test-security/admin")
                                .header(
                                        "Authorization",
                                        "Bearer " + TOKEN
                                )
                )
                .andExpect(
                        status().isForbidden()
                );
    }

    @Test
    void providerScopeCannotCrossSixpayAuthorizationBoundary()
            throws Exception {

        stubOidc(
                new AuthenticatedUser(
                        USER_ID.toString(),
                        "canonical-auditor",
                        Set.of("ROLE_AUDITOR"),
                        false
                )
        );

        mockMvc.perform(
                        get("/test-security/payment-read")
                                .header(
                                        "Authorization",
                                        "Bearer " + TOKEN
                                )
                )
                .andExpect(
                        status().isForbidden()
                );
    }

    @Test
    void sixpayResolvedOidcPermissionAuthorizesRequest()
            throws Exception {

        stubOidc(
                new AuthenticatedUser(
                        USER_ID.toString(),
                        "canonical-manager",
                        Set.of("SCOPE_payment.read"),
                        false
                )
        );

        mockMvc.perform(
                        get("/test-security/payment-read")
                                .header(
                                        "Authorization",
                                        "Bearer " + TOKEN
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        content().string("PAYMENT_READ")
                );
    }

    @Test
    void anonymousRequestDoesNotInvokeOidcIdentityResolution()
            throws Exception {

        mockMvc.perform(
                        get("/test-security/payment-read")
                )
                .andExpect(
                        status().isUnauthorized()
                );

        verifyNoInteractions(
                externalIdentityResolver
        );
    }

    private void stubOidc(
            AuthenticatedUser user
    ) {
        when(
                jwtDecoder.decode(TOKEN)
        )
                .thenReturn(
                        providerJwt()
                );

        when(
                externalIdentityResolver.resolve(
                        any(ExternalIdentity.class)
                )
        )
                .thenReturn(user);
    }

    private static Jwt providerJwt() {
        Instant issuedAt =
                Instant.parse(
                        "2026-08-16T16:00:00Z"
                );

        return Jwt.withTokenValue(TOKEN)
                .header("alg", "RS256")
                .issuer(
                        "https://test-idp.sixpay.local"
                )
                .subject(
                        "provider-user-123"
                )
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
                        List.of("ADMIN", "SUPER_ADMIN")
                )
                .claim(
                        "scope",
                        "payment.read payment.write administration.write"
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
        SecurityBoundaryController securityBoundaryController() {
            return new SecurityBoundaryController();
        }
    }

    @RestController
    static class SecurityBoundaryController {

        @GetMapping("/test-security/admin")
        @PreAuthorize("hasRole('ADMIN')")
        ResponseEntity<String> admin() {
            return ResponseEntity.ok("ADMIN");
        }

        @GetMapping("/test-security/payment-read")
        @PreAuthorize("hasAuthority('SCOPE_payment.read')")
        ResponseEntity<String> paymentRead() {
            return ResponseEntity.ok("PAYMENT_READ");
        }

        @PostMapping("/test-security/admin-mutation")
        @PreAuthorize("hasRole('ADMIN')")
        ResponseEntity<String> adminMutation() {
            return ResponseEntity.ok("MUTATED");
        }
    }
}
