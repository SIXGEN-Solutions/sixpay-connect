package com.sixpay.security.integration;

import com.sixpay.security.application.port.in.AuthenticateLocalUserUseCase;
import com.sixpay.security.application.port.in.ChangeLocalPasswordUseCase;
import com.sixpay.security.application.port.in.LocalLoginCommand;
import com.sixpay.security.application.port.out.SecurityAuditPort;
import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.configuration.SixpaySecurityAutoConfiguration;
import com.sixpay.security.infrastructure.authentication.audit.AuthenticationAuditSpringDataRepository;
import com.sixpay.security.infrastructure.authentication.persistence.LocalAuthenticationUserSpringDataRepository;
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
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DA-11.2 — LOCAL session integration.
 *
 * <p>This test deliberately exercises the real SIXPAY Spring Security filter
 * chain and the real {@code SpringSecuritySessionManager}. Only business and
 * persistence boundaries are mocked.</p>
 */
@SpringBootTest(
        classes =
                LocalAuthenticationSessionIT.TestApplication.class,
        webEnvironment =
                SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "sixpay.security.authentication.local.enabled=true",
                "sixpay.security.authentication.oidc.enabled=false"
        }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LocalAuthenticationSessionIT {

    private static final UUID USER_ID =
            UUID.fromString(
                    "f0383c4b-3d32-3446-81bd-3d45ee0e6721"
            );

    private static final String LOGIN =
            "/api/v1/auth/login";

    private static final String ME =
            "/api/v1/auth/me";

    private static final String LOGOUT =
            "/api/v1/auth/logout";

    private static final String SECURED =
            "/da11/local-session/secured";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticateLocalUserUseCase
            authenticateLocalUserUseCase;

    /*
     * LocalAuthenticationConfiguration also imports the password-change
     * boundary. DA-11.2 is not a password-change use-case test, therefore
     * the application boundary is supplied as a test double.
     */
    @MockitoBean
    private ChangeLocalPasswordUseCase
            changeLocalPasswordUseCase;

    /*
     * Security operational audit remains active through the real controllers
     * and filter chain. Persistence itself is outside DA-11.2.
     */
    @MockitoBean
    private SecurityAuditPort
            securityAuditPort;

    /*
     * LocalAuthenticationConfiguration creates its persistence-backed
     * authentication adapters even though the authenticate use case itself is
     * mocked for this focused HTTP/session test.
     */
    @MockitoBean
    private LocalAuthenticationUserSpringDataRepository
            localAuthenticationUserRepository;

    @MockitoBean
    private AuthenticationAuditSpringDataRepository
            authenticationAuditRepository;

    @Test
    void localLoginCreatesCanonicalSessionSupportsCsrfProtectedMutationAndLogout()
            throws Exception {

        AuthenticatedUser user =
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
                        any(LocalLoginCommand.class)
                )
        )
                .thenReturn(user);

        var loginResult =
                mockMvc.perform(
                                post(LOGIN)
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
                                cookie().exists(
                                        "XSRF-TOKEN"
                                )
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

        verify(
                authenticateLocalUserUseCase
        )
                .authenticate(
                        any(LocalLoginCommand.class)
                );

        MockHttpSession session =
                (MockHttpSession)
                        loginResult
                                .getRequest()
                                .getSession(false);

        assertThat(session)
                .isNotNull();

        Cookie csrfCookie =
                loginResult
                        .getResponse()
                        .getCookie(
                                "XSRF-TOKEN"
                        );

        assertThat(csrfCookie)
                .isNotNull();

        assertThat(
                csrfCookie.getValue()
        )
                .isNotBlank();

        /*
         * The session established by POST /login must be consumable by the
         * mechanism-neutral /auth/me endpoint.
         */
        mockMvc.perform(
                        get(ME)
                                .session(session)
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
                        jsonPath("$.roles[0]")
                                .value("MANAGER")
                )
                .andExpect(
                        jsonPath("$.authenticationMethod")
                                .value("LOCAL")
                )
                .andExpect(
                        jsonPath("$.passwordChangeRequired")
                                .value(false)
                );

        /*
         * Session authentication is reused on subsequent protected GETs.
         */
        mockMvc.perform(
                        get(SECURED)
                                .session(session)
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        content().string(
                                "SECURED"
                        )
                );

        /*
         * Mutating requests backed by a cookie/session remain CSRF protected.
         */
        mockMvc.perform(
                        post(SECURED)
                                .session(session)
                )
                .andExpect(
                        status().isForbidden()
                );

        /*
         * Angular's raw cookie/header CSRF contract must be accepted by the
         * actual SIXPAY filter chain.
         */
        mockMvc.perform(
                        post(SECURED)
                                .session(session)
                                .cookie(csrfCookie)
                                .header(
                                        "X-XSRF-TOKEN",
                                        csrfCookie.getValue()
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        content().string(
                                "SECURED"
                        )
                );

        /*
         * Logout is itself a session mutation and must use the same CSRF
         * contract. The real session manager invalidates the session and
         * clears the CSRF cookie.
         */
        mockMvc.perform(
                        post(LOGOUT)
                                .session(session)
                                .cookie(csrfCookie)
                                .header(
                                        "X-XSRF-TOKEN",
                                        csrfCookie.getValue()
                                )
                )
                .andExpect(
                        status().isNoContent()
                );

        /*
         * No authentication is available after logout when a new request does
         * not carry the terminated session.
         */
        mockMvc.perform(
                        get(SECURED)
                )
                .andExpect(
                        status().isUnauthorized()
                );
    }

    @Test
    void localMustChangeCredentialCreatesRestrictedSession()
            throws Exception {

        AuthenticatedUser restrictedUser =
                new AuthenticatedUser(
                        USER_ID.toString(),
                        "manager",
                        Set.of(
                                "ROLE_MANAGER",
                                "SCOPE_payment.read"
                        ),
                        true
                );

        when(
                authenticateLocalUserUseCase.authenticate(
                        any(LocalLoginCommand.class)
                )
        )
                .thenReturn(
                        restrictedUser
                );

        var loginResult =
                mockMvc.perform(
                                post(LOGIN)
                                        .contentType(
                                                MediaType.APPLICATION_JSON
                                        )
                                        .content(
                                                """
                                                {
                                                  "username": "manager",
                                                  "password": "temporary-password-2027"
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
                                jsonPath("$.authenticationMethod")
                                        .value("LOCAL")
                        )
                        .andExpect(
                                jsonPath("$.passwordChangeRequired")
                                        .value(true)
                        )
                        .andReturn();

        MockHttpSession session =
                (MockHttpSession)
                        loginResult
                                .getRequest()
                                .getSession(false);

        assertThat(session)
                .isNotNull();

        /*
         * /auth/me remains reachable so the SPA can discover the restricted
         * lifecycle state.
         */
        mockMvc.perform(
                        get(ME)
                                .session(session)
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.passwordChangeRequired")
                                .value(true)
                );

        /*
         * Business resources are denied until the LOCAL password lifecycle is
         * remediated.
         */
        mockMvc.perform(
                        get(SECURED)
                                .session(session)
                )
                .andExpect(
                        status().isForbidden()
                );
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(
            exclude = {
                    DataSourceAutoConfiguration.class,
                    HibernateJpaAutoConfiguration.class,
                    DataJpaRepositoriesAutoConfiguration.class
            }
    )
    @Import(
            SixpaySecurityAutoConfiguration.class
    )
    static class TestApplication {

        @Bean
        TestProtectedController
        da11LocalSessionProtectedController() {
            return new TestProtectedController();
        }
    }

    @RestController
    static class TestProtectedController {

        @GetMapping(SECURED)
        ResponseEntity<String> securedGet() {
            return ResponseEntity.ok(
                    "SECURED"
            );
        }

        @PostMapping(SECURED)
        ResponseEntity<String> securedPost() {
            return ResponseEntity.ok(
                    "SECURED"
            );
        }
    }
}
