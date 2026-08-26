package com.sixpay.security.api.controller;

import com.sixpay.security.api.dto.LocalPasswordChangeRequest;
import com.sixpay.security.application.port.in.ChangeLocalPasswordCommand;
import com.sixpay.security.application.port.in.ChangeLocalPasswordUseCase;
import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.authentication.CurrentUserProvider;
import com.sixpay.security.domain.authentication.AuthenticationMethod;
import com.sixpay.security.infrastructure.authentication.session.SpringSecuritySessionManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;
import java.util.UUID;

/**
 * Authenticated user-owned LOCAL credential operations.
 *
 * <p>Administrative reset remains under the administration API and is not
 * exposed through this controller.</p>
 */
@RestController
@RequestMapping("/api/v1/auth/password")
@ConditionalOnProperty(
        prefix =
                "sixpay.security.authentication.local",
        name = "enabled",
        havingValue = "true"
)
public final class LocalPasswordController {

    private final ChangeLocalPasswordUseCase
            changeLocalPassword;

    private final CurrentUserProvider
            currentUserProvider;

    private final SpringSecuritySessionManager
            sessionManager;

    public LocalPasswordController(
            ChangeLocalPasswordUseCase changeLocalPassword,
            CurrentUserProvider currentUserProvider,
            SpringSecuritySessionManager sessionManager
    ) {
        this.changeLocalPassword =
                Objects.requireNonNull(
                        changeLocalPassword
                );
        this.currentUserProvider =
                Objects.requireNonNull(
                        currentUserProvider
                );
        this.sessionManager =
                Objects.requireNonNull(
                        sessionManager
                );
    }

    @PostMapping("/change")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody
            LocalPasswordChangeRequest requestBody,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        /*
         * OIDC passwords belong to the IdP. Even if the same canonical user
         * also has a LOCAL credential, an OIDC session is not a LOCAL
         * current-password proof.
         */
        AuthenticationMethod method =
                sessionManager
                        .currentAuthenticationMethod(
                                request
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "Authentication method is unavailable"
                                )
                        );

        if (method != AuthenticationMethod.LOCAL) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "LOCAL authentication is required to change a LOCAL password"
            );
        }

        AuthenticatedUser currentUser =
                currentUserProvider
                        .requireCurrentUser();

        UUID userId =
                canonicalUserId(
                        currentUser.subject()
                );

        changeLocalPassword.changePassword(
                new ChangeLocalPasswordCommand(
                        userId,
                        currentUser.subject(),
                        requestBody.currentPassword(),
                        requestBody.newPassword()
                )
        );

        /*
         * The database lifecycle is now normal. Promote the same authenticated
         * session immediately so the user does not need to log out and back in.
         */
        sessionManager
                .completeLocalPasswordChange(
                        request,
                        response
                );

        return ResponseEntity
                .noContent()
                .build();
    }

    private static UUID canonicalUserId(
            String subject
    ) {
        try {
            return UUID.fromString(
                    subject
            );
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "Authenticated subject is not a canonical SIXPAY user id",
                    exception
            );
        }
    }
}
