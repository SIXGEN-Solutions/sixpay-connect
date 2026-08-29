package com.sixpay.security.api.controller;

import com.sixpay.security.application.port.input.ChangeLocalPasswordUseCase;
import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.authentication.CurrentUserProvider;
import com.sixpay.security.domain.authentication.AuthenticationMethod;
import com.sixpay.security.infrastructure.authentication.session.SpringSecuritySessionManager;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = LocalPasswordController.class,
        properties = {
                "sixpay.security.authentication.local.enabled=true"
        }
)
@ContextConfiguration(
        classes = {
                LocalPasswordController.class,
                PasswordChangeControllerIT.SecurityTestConfiguration.class
        }
)
class PasswordChangeControllerIT {

    private static final UUID USER_ID =
            UUID.fromString(
                    "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
            );

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChangeLocalPasswordUseCase useCase;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    @MockitoBean
    private SpringSecuritySessionManager sessionManager;

    @Test
    @WithMockUser(
            username = "manager",
            roles = "MANAGER"
    )
    void localSessionChangesPasswordAndPromotesSameSession()
            throws Exception {

        when(
                currentUserProvider
                        .requireCurrentUser()
        )
                .thenReturn(
                        new AuthenticatedUser(
                                USER_ID.toString(),
                                "manager",
                                Set.of(
                                        "ROLE_MANAGER"
                                ),
                                true
                        )
                );

        when(
                sessionManager
                        .currentAuthenticationMethod(
                                any(HttpServletRequest.class)
                        )
        )
                .thenReturn(
                        Optional.of(
                                AuthenticationMethod.LOCAL
                        )
                );

        mockMvc.perform(
                        post(
                                "/api/v1/auth/password/change"
                        )
                                .with(csrf())
                                .contentType(
                                        "application/json"
                                )
                                .content(
                                        """
                                        {
                                          "currentPassword": "Temporary-password-2026",
                                          "newPassword": "Permanent-password-2027"
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isNoContent()
                );

        verify(useCase)
                .changePassword(
                        argThat(command ->
                                command.userId()
                                        .equals(USER_ID)
                                        && command.actorSubject()
                                        .equals(
                                                USER_ID.toString()
                                        )
                                        && command.currentPassword()
                                        .equals(
                                                "Temporary-password-2026"
                                        )
                                        && command.newPassword()
                                        .equals(
                                                "Permanent-password-2027"
                                        )
                        )
                );

        verify(sessionManager)
                .completeLocalPasswordChange(
                        any(),
                        any()
                );
    }

    @Test
    @WithMockUser(
            username = "manager",
            roles = "MANAGER"
    )
    void oidcSessionCannotUseSixpayLocalPasswordLifecycle()
            throws Exception {

        when(
                sessionManager
                        .currentAuthenticationMethod(
                                any(HttpServletRequest.class)
                        )
        )
                .thenReturn(
                        Optional.of(
                                AuthenticationMethod.OIDC
                        )
                );

        mockMvc.perform(
                        post(
                                "/api/v1/auth/password/change"
                        )
                                .with(csrf())
                                .contentType(
                                        "application/json"
                                )
                                .content(
                                        """
                                        {
                                          "currentPassword": "Idp-password-2026",
                                          "newPassword": "Permanent-password-2027"
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isConflict()
                );

        verifyNoInteractions(
                useCase
        );

        verifyNoInteractions(
                currentUserProvider
        );
    }

    @Test
    @WithMockUser(
            username = "manager",
            roles = "MANAGER"
    )
    void validationRejectsBlankNewPasswordBeforeUseCase()
            throws Exception {

        mockMvc.perform(
                        post(
                                "/api/v1/auth/password/change"
                        )
                                .with(csrf())
                                .contentType(
                                        "application/json"
                                )
                                .content(
                                        """
                                        {
                                          "currentPassword": "Temporary-password-2026",
                                          "newPassword": ""
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isBadRequest()
                );

        verifyNoInteractions(
                useCase
        );

        verifyNoInteractions(
                currentUserProvider
        );

        verifyNoInteractions(
                sessionManager
        );
    }

    @Test
    @WithMockUser(
            username = "manager",
            roles = "MANAGER"
    )
    void validationRejectsBlankCurrentPasswordBeforeUseCase()
            throws Exception {

        mockMvc.perform(
                        post(
                                "/api/v1/auth/password/change"
                        )
                                .with(csrf())
                                .contentType(
                                        "application/json"
                                )
                                .content(
                                        """
                                        {
                                          "currentPassword": "",
                                          "newPassword": "Permanent-password-2027"
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isBadRequest()
                );

        verifyNoInteractions(
                useCase
        );

        verifyNoInteractions(
                currentUserProvider
        );

        verifyNoInteractions(
                sessionManager
        );
    }

    @Configuration(
            proxyBeanMethods = false
    )
    @EnableMethodSecurity
    @Import(
            LocalPasswordController.class
    )
    static class SecurityTestConfiguration {
    }
}