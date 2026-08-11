package com.sixpay.security.infrastructure.authentication.session;

import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.domain.authentication.AuthenticationMethod;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;

import java.util.Objects;
import java.util.Optional;

/**
 * Mechanism-neutral SIXPAY backend session manager.
 *
 * <p>Both Local and OIDC authentication converge here after identity and
 * authorization have been resolved to the canonical SIXPAY principal.</p>
 */
public class SpringSecuritySessionManager {

    private static final String AUTHENTICATION_METHOD_ATTRIBUTE =
            "com.sixpay.security.authentication.method";

    private final SecurityContextRepository securityContextRepository;
    private final CsrfTokenRepository csrfTokenRepository;

    public SpringSecuritySessionManager(
            SecurityContextRepository securityContextRepository,
            CsrfTokenRepository csrfTokenRepository
    ) {
        this.securityContextRepository = Objects.requireNonNull(securityContextRepository);
        this.csrfTokenRepository = Objects.requireNonNull(csrfTokenRepository);
    }

    public void startSession(
            AuthenticatedUser user,
            AuthenticationMethod authenticationMethod,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        Objects.requireNonNull(user, "Authenticated user must not be null");
        Objects.requireNonNull(authenticationMethod, "Authentication method must not be null");

        HttpSession existingSession = request.getSession(false);
        if (existingSession != null) {
            existingSession.invalidate();
        }

        var authorities = user.authorities()
                .stream()
                .map(SimpleGrantedAuthority::new)
                .toList();

        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                user,
                null,
                authorities
        );

        var securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        securityContextRepository.saveContext(
                securityContext,
                request,
                response
        );

        request.getSession(true).setAttribute(
                AUTHENTICATION_METHOD_ATTRIBUTE,
                authenticationMethod.name()
        );

        CsrfToken token = csrfTokenRepository.generateToken(request);
        csrfTokenRepository.saveToken(token, request, response);
    }

    public Optional<AuthenticationMethod> currentAuthenticationMethod(
            HttpServletRequest request
    ) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return Optional.empty();
        }

        Object value = session.getAttribute(AUTHENTICATION_METHOD_ATTRIBUTE);
        if (!(value instanceof String method) || method.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(AuthenticationMethod.valueOf(method));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public void terminateSession(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        SecurityContextHolder.clearContext();

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        csrfTokenRepository.saveToken(null, request, response);
    }
}
