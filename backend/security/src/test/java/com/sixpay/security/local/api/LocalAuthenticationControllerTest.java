package com.sixpay.security.local.api;

import com.sixpay.security.local.LocalPrincipal;
import com.sixpay.security.local.LocalRole;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LocalAuthenticationControllerTest {

    private final AuthenticationManager authenticationManager =
            mock(AuthenticationManager.class);

    private final HttpSessionSecurityContextRepository contextRepository =
            new HttpSessionSecurityContextRepository();

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(
                    new LocalAuthenticationController(
                            authenticationManager,
                            contextRepository
                    )
            )
            .build();

    @Test
    void loginReturnsCurrentUserAndCreatesSessionSecurityContext()
            throws Exception {
        var principal = new LocalPrincipal(
                "admin",
                "{bcrypt}",
                "local-admin",
                true,
                Set.of(LocalRole.ADMIN),
                List.of(
                        new SimpleGrantedAuthority("ROLE_ADMIN"),
                        new SimpleGrantedAuthority("SCOPE_payment.read")
                )
        );
        var authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        null,
                        principal.getAuthorities()
                );

        when(authenticationManager.authenticate(any()))
                .thenReturn(authentication);

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "username": "admin",
                                          "password": "admin-dev-2026"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("local-admin"))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"));
    }

    @Test
    void currentUserReturnsAuthenticatedPrincipal() throws Exception {
        var principal = new LocalPrincipal(
                "auditor",
                "{bcrypt}",
                "local-auditor",
                true,
                Set.of(LocalRole.AUDITOR),
                List.of(new SimpleGrantedAuthority("ROLE_AUDITOR"))
        );
        var authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        null,
                        principal.getAuthorities()
                );

        mockMvc.perform(
                        get("/api/v1/auth/me")
                                .principal(authentication)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("local-auditor"))
                .andExpect(jsonPath("$.roles[0]").value("AUDITOR"));
    }

    @Test
    void logoutInvalidatesExistingSession() throws Exception {
        var session = new MockHttpSession();

        mockMvc.perform(
                        post("/api/v1/auth/logout")
                                .session(session)
                )
                .andExpect(status().isNoContent());

        org.assertj.core.api.Assertions.assertThat(session.isInvalid()).isTrue();
    }
}
