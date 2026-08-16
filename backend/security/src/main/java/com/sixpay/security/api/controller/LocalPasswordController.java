package com.sixpay.security.api.controller;

import com.sixpay.security.api.dto.LocalPasswordChangeRequest;
import com.sixpay.security.application.port.in.ChangeLocalPasswordCommand;
import com.sixpay.security.application.port.in.ChangeLocalPasswordUseCase;
import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.authentication.CurrentUserProvider;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        prefix = "sixpay.security.authentication.local",
        name = "enabled",
        havingValue = "true"
)
public final class LocalPasswordController {

    private final ChangeLocalPasswordUseCase changeLocalPassword;
    private final CurrentUserProvider currentUserProvider;

    public LocalPasswordController(
            ChangeLocalPasswordUseCase changeLocalPassword,
            CurrentUserProvider currentUserProvider
    ) {
        this.changeLocalPassword =
                Objects.requireNonNull(changeLocalPassword);
        this.currentUserProvider =
                Objects.requireNonNull(currentUserProvider);
    }

    @PostMapping("/change")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody
            LocalPasswordChangeRequest request
    ) {
        AuthenticatedUser currentUser =
                currentUserProvider.requireCurrentUser();

        UUID userId =
                canonicalUserId(
                        currentUser.subject()
                );

        changeLocalPassword.changePassword(
                new ChangeLocalPasswordCommand(
                        userId,
                        currentUser.subject(),
                        request.currentPassword(),
                        request.newPassword()
                )
        );

        return ResponseEntity.noContent().build();
    }

    private static UUID canonicalUserId(
            String subject
    ) {
        try {
            return UUID.fromString(subject);
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "Authenticated subject is not a canonical SIXPAY user id",
                    exception
            );
        }
    }
}
