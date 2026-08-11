package com.sixpay.security.api.controller;

import com.sixpay.security.api.dto.AuthenticationSessionResponse;
import com.sixpay.security.application.port.in.GetCurrentSessionUseCase;
import com.sixpay.security.application.port.out.SecurityAuditPort;
import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.domain.administration.SecurityAuditEvent;
import com.sixpay.security.domain.administration.SecurityAuditEventType;
import com.sixpay.security.domain.authentication.AuthenticationMethod;
import com.sixpay.security.infrastructure.authentication.oidc.OidcAuthenticationToken;
import com.sixpay.security.infrastructure.authentication.session.SpringSecuritySessionManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public final class AuthenticationSessionController {

    private final GetCurrentSessionUseCase getCurrentSession;
    private final SpringSecuritySessionManager sessionManager;
    private final SecurityAuditPort auditPort;

    public AuthenticationSessionController(
            GetCurrentSessionUseCase getCurrentSession,
            SpringSecuritySessionManager sessionManager,
            SecurityAuditPort auditPort
    ) {
        this.getCurrentSession = Objects.requireNonNull(getCurrentSession);
        this.sessionManager = Objects.requireNonNull(sessionManager);
        this.auditPort = Objects.requireNonNull(auditPort);
    }

    @GetMapping("/me")
    public AuthenticationSessionResponse currentSession(HttpServletRequest request) {
        AuthenticatedUser user = getCurrentSession.getCurrentSession();
        AuthenticationMethod method = sessionManager.currentAuthenticationMethod(request)
                .orElseGet(AuthenticationSessionController::inferCurrentMethod);
        return toResponse(user, method);
    }

    @PostMapping("/session/oidc")
    public AuthenticationSessionResponse establishOidcSession(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof OidcAuthenticationToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "OIDC bearer authentication is required");
        }

        AuthenticatedUser user = getCurrentSession.getCurrentSession();
        sessionManager.startSession(user, AuthenticationMethod.OIDC, request, response);
        return toResponse(user, AuthenticationMethod.OIDC);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        AuthenticatedUser currentUser = getCurrentSession.getCurrentSession();
        auditPort.record(new SecurityAuditEvent(
                SecurityAuditEventType.LOGOUT,
                currentUser.subject(),
                parseUserId(currentUser.subject()),
                currentUser.username(),
                null,
                null,
                Instant.now()
        ));
        sessionManager.terminateSession(request, response);
        return ResponseEntity.noContent().build();
    }

    static AuthenticationSessionResponse toResponse(
            AuthenticatedUser user,
            AuthenticationMethod method
    ) {
        return new AuthenticationSessionResponse(
                user.subject(), user.username(), user.roles(), user.permissions(), method
        );
    }

    private static AuthenticationMethod inferCurrentMethod() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication instanceof OidcAuthenticationToken
                ? AuthenticationMethod.OIDC
                : AuthenticationMethod.LOCAL;
    }

    private static UUID parseUserId(String subject) {
        try { return UUID.fromString(subject); }
        catch (RuntimeException ignored) { return null; }
    }
}
