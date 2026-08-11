package com.sixpay.security.infrastructure.authentication.session;

import com.sixpay.security.authentication.AuthenticatedUser;
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

public final class SpringSecurityLocalSessionManager {

    private final SecurityContextRepository securityContextRepository;
    private final CsrfTokenRepository csrfTokenRepository;

    public SpringSecurityLocalSessionManager(
            SecurityContextRepository securityContextRepository,
            CsrfTokenRepository csrfTokenRepository
    ) {
        this.securityContextRepository =
                Objects.requireNonNull(securityContextRepository);
        this.csrfTokenRepository =
                Objects.requireNonNull(csrfTokenRepository);
    }

    public void startSession(
            AuthenticatedUser user,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        HttpSession existing = request.getSession(false);

        if (existing != null) {
            existing.invalidate();
        }

        var authorities = user.authorities()
                .stream()
                .map(SimpleGrantedAuthority::new)
                .toList();

        var authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        user,
                        null,
                        authorities
                );

        var securityContext =
                SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        securityContextRepository.saveContext(
                securityContext,
                request,
                response
        );

        CsrfToken token =
                csrfTokenRepository.generateToken(request);

        csrfTokenRepository.saveToken(
                token,
                request,
                response
        );
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

        csrfTokenRepository.saveToken(
                null,
                request,
                response
        );
    }
}
