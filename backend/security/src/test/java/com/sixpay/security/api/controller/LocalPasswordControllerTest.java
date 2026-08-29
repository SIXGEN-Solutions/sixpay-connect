package com.sixpay.security.api.controller;

import com.sixpay.security.api.dto.LocalPasswordChangeRequest;
import com.sixpay.security.application.port.input.ChangeLocalPasswordCommand;
import com.sixpay.security.application.port.input.ChangeLocalPasswordUseCase;
import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.authentication.CurrentUserProvider;
import com.sixpay.security.domain.authentication.AuthenticationMethod;
import com.sixpay.security.infrastructure.authentication.session.SpringSecuritySessionManager;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class LocalPasswordControllerTest {

    private static final UUID USER_ID =
            UUID.fromString(
                    "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
            );

    @Test
    void changesPasswordForAuthenticatedLocalUserAndPromotesSession() {

        ChangeLocalPasswordUseCase useCase =
                mock(
                        ChangeLocalPasswordUseCase.class
                );

        CurrentUserProvider currentUserProvider =
                mock(
                        CurrentUserProvider.class
                );

        SpringSecuritySessionManager sessionManager =
                mock(
                        SpringSecuritySessionManager.class
                );

        MockHttpServletRequest servletRequest =
                new MockHttpServletRequest();

        MockHttpServletResponse servletResponse =
                new MockHttpServletResponse();

        AuthenticatedUser authenticatedUser =
                new AuthenticatedUser(
                        USER_ID.toString(),
                        "admin",
                        Set.of(
                                "ROLE_ADMIN"
                        ),
                        true
                );

        when(
                currentUserProvider
                        .requireCurrentUser()
        )
                .thenReturn(
                        authenticatedUser
                );

        when(
                sessionManager
                        .currentAuthenticationMethod(
                                servletRequest
                        )
        )
                .thenReturn(
                        java.util.Optional.of(
                                AuthenticationMethod.LOCAL
                        )
                );

        LocalPasswordController controller =
                new LocalPasswordController(
                        useCase,
                        currentUserProvider,
                        sessionManager
                );

        var response =
                controller.changePassword(
                        new LocalPasswordChangeRequest(
                                "Current-password-2026",
                                "Brand-new-password-2026"
                        ),
                        servletRequest,
                        servletResponse
                );

        assertThat(
                response.getStatusCode()
        )
                .isEqualTo(
                        HttpStatus.NO_CONTENT
                );

        ArgumentCaptor<ChangeLocalPasswordCommand> command =
                ArgumentCaptor.forClass(
                        ChangeLocalPasswordCommand.class
                );

        verify(useCase)
                .changePassword(
                        command.capture()
                );

        assertThat(
                command.getValue()
                        .userId()
        )
                .isEqualTo(
                        USER_ID
                );

        assertThat(
                command.getValue()
                        .actorSubject()
        )
                .isEqualTo(
                        USER_ID.toString()
                );

        verify(sessionManager)
                .completeLocalPasswordChange(
                        servletRequest,
                        servletResponse
                );
    }

    @Test
    void doesNotApplyLocalPasswordLifecycleToOidcSession() {

        ChangeLocalPasswordUseCase useCase =
                mock(
                        ChangeLocalPasswordUseCase.class
                );

        CurrentUserProvider currentUserProvider =
                mock(
                        CurrentUserProvider.class
                );

        SpringSecuritySessionManager sessionManager =
                mock(
                        SpringSecuritySessionManager.class
                );

        MockHttpServletRequest servletRequest =
                new MockHttpServletRequest();

        when(
                sessionManager
                        .currentAuthenticationMethod(
                                servletRequest
                        )
        )
                .thenReturn(
                        java.util.Optional.of(
                                AuthenticationMethod.OIDC
                        )
                );

        LocalPasswordController controller =
                new LocalPasswordController(
                        useCase,
                        currentUserProvider,
                        sessionManager
                );

        org.assertj.core.api.Assertions
                .assertThatThrownBy(() ->
                        controller.changePassword(
                                new LocalPasswordChangeRequest(
                                        "idp-password",
                                        "new-password-2026"
                                ),
                                servletRequest,
                                new MockHttpServletResponse()
                        )
                )
                .isInstanceOf(
                        org.springframework.web.server.ResponseStatusException.class
                );

        verifyNoInteractions(
                useCase
        );

        verifyNoInteractions(
                currentUserProvider
        );
    }
}
