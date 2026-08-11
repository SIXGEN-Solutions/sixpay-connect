package com.sixpay.security.api.controller;

import com.sixpay.security.api.dto.AuthenticationSessionResponse;
import com.sixpay.security.application.port.in.GetCurrentSessionUseCase;
import com.sixpay.security.application.port.in.LogoutUseCase;
import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.domain.authentication.AuthenticationMethod;
import com.sixpay.security.infrastructure.authentication.oidc.OidcAuthenticationToken;
import com.sixpay.security.infrastructure.authentication.session.SpringSecuritySessionManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

/**
 * Mechanism-neutral SIXPAY backend session boundary.
 */
@RestController
@RequestMapping("/api/v1/auth")
public final class AuthenticationSessionController {

    private final GetCurrentSessionUseCase getCurrentSession;
    private final SpringSecuritySessionManager sessionManager;
    private final ObjectProvider<LogoutUseCase> logoutUseCaseProvider;

    public AuthenticationSessionController(
            GetCurrentSessionUseCase getCurrentSession,
            SpringSecuritySessionManager sessionManager,
            ObjectProvider<LogoutUseCase> logoutUseCaseProvider
    ) {
        this.getCurrentSession = Objects.requireNonNull(getCurrentSession);
        this.sessionManager = Objects.requireNonNull(sessionManager);
        this.logoutUseCaseProvider = Objects.requireNonNull(logoutUseCaseProvider);
    }

    @GetMapping("/me")
    public AuthenticationSessionResponse currentSession(
            HttpServletRequest request
    ) {
        AuthenticatedUser user = getCurrentSession.getCurrentSession();

        AuthenticationMethod authenticationMethod =
                sessionManager.currentAuthenticationMethod(request)
                        .orElseGet(AuthenticationSessionController::inferCurrentMethod);

        return toResponse(user, authenticationMethod);
    }

    /**
     * Converts the already validated OIDC bearer authentication into the same
     * server-side SIXPAY session used by Local authentication.
     */
    @PostMapping("/session/oidc")
    public AuthenticationSessionResponse establishOidcSession(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof OidcAuthenticationToken)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "OIDC bearer authentication is required"
            );
        }

        AuthenticatedUser user = getCurrentSession.getCurrentSession();

        sessionManager.startSession(
                user,
                AuthenticationMethod.OIDC,
                request,
                response
        );

        return toResponse(user, AuthenticationMethod.OIDC);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        AuthenticatedUser currentUser = getCurrentSession.getCurrentSession();

        LogoutUseCase logoutUseCase = logoutUseCaseProvider.getIfAvailable();
        if (logoutUseCase != null) {
            logoutUseCase.logout(currentUser);
        }

        sessionManager.terminateSession(request, response);

        return ResponseEntity.noContent().build();
    }

    static AuthenticationSessionResponse toResponse(
            AuthenticatedUser user,
            AuthenticationMethod authenticationMethod
    ) {
        return new AuthenticationSessionResponse(
                user.subject(),
                user.username(),
                user.roles(),
                user.permissions(),
                authenticationMethod
        );
    }

    private static AuthenticationMethod inferCurrentMethod() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        return authentication instanceof OidcAuthenticationToken
                ? AuthenticationMethod.OIDC
                : AuthenticationMethod.LOCAL;
    }
}
