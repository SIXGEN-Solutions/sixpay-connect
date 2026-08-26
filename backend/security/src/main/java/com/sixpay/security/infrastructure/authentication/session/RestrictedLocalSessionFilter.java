package com.sixpay.security.infrastructure.authentication.session;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

/**
 * Restricts an authenticated LOCAL session whose credential is expired or
 * marked must-change-password.
 *
 * <p>Only the session inspection, logout and password-change endpoints remain
 * available. OIDC sessions never enter this path.</p>
 */
public final class RestrictedLocalSessionFilter
        extends OncePerRequestFilter {

    private static final String AUTH_ME =
            "/api/v1/auth/me";

    private static final String AUTH_LOGOUT =
            "/api/v1/auth/logout";

    private static final String PASSWORD_CHANGE =
            "/api/v1/auth/password/change";

    private final SpringSecuritySessionManager
            sessionManager;

    public RestrictedLocalSessionFilter(
            SpringSecuritySessionManager sessionManager
    ) {
        this.sessionManager =
                Objects.requireNonNull(
                        sessionManager
                );
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if (!sessionManager
                .passwordChangeRequired(
                        request
                )
                || isAllowed(request)) {
            filterChain.doFilter(
                    request,
                    response
            );
            return;
        }

        throw new AccessDeniedException(
                "LOCAL password change is required before accessing this resource"
        );
    }

    private static boolean isAllowed(
            HttpServletRequest request
    ) {
        String path =
                request.getRequestURI();

        String method =
                request.getMethod();

        return ("GET".equals(method)
                && AUTH_ME.equals(path))
                || ("POST".equals(method)
                && AUTH_LOGOUT.equals(path))
                || ("POST".equals(method)
                && PASSWORD_CHANGE.equals(path));
    }
}
