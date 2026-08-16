package com.sixpay.security.api.controller;

import com.sixpay.security.api.dto.LocalPasswordChangeRequest;
import com.sixpay.security.application.port.in.ChangeLocalPasswordCommand;
import com.sixpay.security.application.port.in.ChangeLocalPasswordUseCase;
import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.authentication.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocalPasswordControllerTest {

    private static final UUID USER_ID =
            UUID.fromString(
                    "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
            );

    @Test
    void changesPasswordForAuthenticatedCanonicalUser() {

        ChangeLocalPasswordUseCase useCase =
                mock(
                        ChangeLocalPasswordUseCase.class
                );

        CurrentUserProvider currentUserProvider =
                mock(
                        CurrentUserProvider.class
                );

        AuthenticatedUser authenticatedUser =
                new AuthenticatedUser(
                        USER_ID.toString(),
                        "admin",
                        Set.of(
                                "ROLE_ADMIN"
                        )
                );

        /*
         * LocalPasswordController calls requireCurrentUser(),
         * not currentUser().
         *
         * Because CurrentUserProvider is a Mockito mock,
         * explicitly stub the method actually invoked by
         * the controller.
         */
        when(
                currentUserProvider.requireCurrentUser()
        )
                .thenReturn(
                        authenticatedUser
                );

        LocalPasswordController controller =
                new LocalPasswordController(
                        useCase,
                        currentUserProvider
                );

        var response =
                controller.changePassword(
                        new LocalPasswordChangeRequest(
                                "Current-password-2026",
                                "Brand-new-password-2026"
                        )
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

        verify(
                useCase
        )
                .changePassword(
                        command.capture()
                );

        ChangeLocalPasswordCommand captured =
                command.getValue();

        assertThat(
                captured.userId()
        )
                .isEqualTo(
                        USER_ID
                );

        assertThat(
                captured.actorSubject()
        )
                .isEqualTo(
                        USER_ID.toString()
                );

        assertThat(
                captured.currentPassword()
        )
                .isEqualTo(
                        "Current-password-2026"
                );

        assertThat(
                captured.newPassword()
        )
                .isEqualTo(
                        "Brand-new-password-2026"
                );

        verify(
                currentUserProvider
        )
                .requireCurrentUser();
    }
}