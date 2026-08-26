package com.sixpay.security.api.controller;

import com.sixpay.security.api.dto.AuthenticationSessionResponse;
import com.sixpay.security.api.dto.LocalLoginRequest;
import com.sixpay.security.application.port.in.AuthenticateLocalUserUseCase;
import com.sixpay.security.application.port.in.LocalLoginCommand;
import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.domain.authentication.AuthenticationMethod;
import com.sixpay.security.infrastructure.authentication.session.SpringSecuritySessionManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * Local credential boundary. Session read/logout are mechanism-neutral and
 * owned by {@link AuthenticationSessionController}.
 */
@RestController
@RequestMapping("/api/v1/auth")
@ConditionalOnProperty(
        prefix = "sixpay.security.authentication.local",
        name = "enabled",
        havingValue = "true"
)
public final class LocalAuthenticationController {

    private final AuthenticateLocalUserUseCase authenticateLocalUser;
    private final SpringSecuritySessionManager sessionManager;

    public LocalAuthenticationController(
            AuthenticateLocalUserUseCase authenticateLocalUser,
            SpringSecuritySessionManager sessionManager
    ) {
        this.authenticateLocalUser = Objects.requireNonNull(authenticateLocalUser);
        this.sessionManager = Objects.requireNonNull(sessionManager);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationSessionResponse> login(
            @Valid @RequestBody LocalLoginRequest requestBody,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        AuthenticatedUser authenticatedUser =
                authenticateLocalUser.authenticate(
                        new LocalLoginCommand(
                                requestBody.username(),
                                requestBody.password()
                        )
                );

        sessionManager.startSession(
                authenticatedUser,
                AuthenticationMethod.LOCAL,
                request,
                response
        );

        return ResponseEntity.ok(
                AuthenticationSessionController.toResponse(
                        authenticatedUser,
                        AuthenticationMethod.LOCAL
                )
        );
    }
}
