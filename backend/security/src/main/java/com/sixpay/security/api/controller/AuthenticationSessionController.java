package com.sixpay.security.api.controller;

import com.sixpay.security.api.dto.AuthenticationSessionResponse;
import com.sixpay.security.application.port.in.GetCurrentSessionUseCase;
import com.sixpay.security.authentication.AuthenticatedUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * Mechanism-neutral current-session endpoint.
 *
 * <p>Both Local and OIDC callers receive authorization resolved by SIXPAY.</p>
 */
@RestController
@RequestMapping("/api/v1/auth")
public final class AuthenticationSessionController {

    private final GetCurrentSessionUseCase getCurrentSession;

    public AuthenticationSessionController(
            GetCurrentSessionUseCase getCurrentSession
    ) {
        this.getCurrentSession =
                Objects.requireNonNull(getCurrentSession);
    }

    @GetMapping("/me")
    public AuthenticationSessionResponse currentSession() {
        return toResponse(
                getCurrentSession.getCurrentSession()
        );
    }

    static AuthenticationSessionResponse toResponse(
            AuthenticatedUser user
    ) {
        return new AuthenticationSessionResponse(
                user.subject(),
                user.username(),
                user.roles(),
                user.permissions()
        );
    }
}
