package com.sixpay.security.api.controller;

import com.sixpay.security.api.dto.LocalLoginRequest;
import com.sixpay.security.api.dto.LocalSessionResponse;
import com.sixpay.security.application.port.in.AuthenticateLocalUserUseCase;
import com.sixpay.security.application.port.in.GetCurrentSessionUseCase;
import com.sixpay.security.application.port.in.LocalLoginCommand;
import com.sixpay.security.application.port.in.LogoutUseCase;
import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.infrastructure.authentication.session.SpringSecurityLocalSessionManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/auth")
public final class LocalAuthenticationController {

    private final AuthenticateLocalUserUseCase authenticateLocalUser;
    private final GetCurrentSessionUseCase getCurrentSession;
    private final LogoutUseCase logoutUseCase;
    private final SpringSecurityLocalSessionManager sessionManager;

    public LocalAuthenticationController(
            AuthenticateLocalUserUseCase authenticateLocalUser,
            GetCurrentSessionUseCase getCurrentSession,
            LogoutUseCase logoutUseCase,
            SpringSecurityLocalSessionManager sessionManager
    ) {
        this.authenticateLocalUser = Objects.requireNonNull(authenticateLocalUser);
        this.getCurrentSession = Objects.requireNonNull(getCurrentSession);
        this.logoutUseCase = Objects.requireNonNull(logoutUseCase);
        this.sessionManager = Objects.requireNonNull(sessionManager);
    }

    @PostMapping("/login")
    public ResponseEntity<LocalSessionResponse> login(
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
                request,
                response
        );

        return ResponseEntity.ok(toResponse(authenticatedUser));
    }

    @GetMapping("/me")
    public LocalSessionResponse currentSession() {
        return toResponse(
                getCurrentSession.getCurrentSession()
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        AuthenticatedUser currentUser =
                getCurrentSession.getCurrentSession();

        logoutUseCase.logout(currentUser);
        sessionManager.terminateSession(request, response);

        return ResponseEntity.noContent().build();
    }

    private static LocalSessionResponse toResponse(
            AuthenticatedUser user
    ) {
        return new LocalSessionResponse(
                user.subject(),
                user.username(),
                user.roles()
        );
    }
}
