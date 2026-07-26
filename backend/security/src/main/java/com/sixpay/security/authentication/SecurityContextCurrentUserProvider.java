package com.sixpay.security.authentication;

import org.springframework.security.authentication
        .AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reads the authenticated user from Spring Security's context.
 */
public final class SecurityContextCurrentUserProvider
        implements CurrentUserProvider {

    private static final String PREFERRED_USERNAME =
            "preferred_username";

    private static final String EMAIL = "email";

    @Override
    public Optional<AuthenticatedUser> currentUser() {
        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        if (!isAuthenticated(authentication)) {
            return Optional.empty();
        }

        String subject = resolveSubject(authentication);
        String username = resolveUsername(
                authentication,
                subject
        );

        Set<String> authorities = authentication
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toUnmodifiableSet());

        return Optional.of(
                new AuthenticatedUser(
                        subject,
                        username,
                        authorities
                )
        );
    }

    private boolean isAuthenticated(
            Authentication authentication
    ) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication
                instanceof AnonymousAuthenticationToken);
    }

    private String resolveSubject(
            Authentication authentication
    ) {
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        }

        return authentication.getName();
    }

    private String resolveUsername(
            Authentication authentication,
            String subject
    ) {
        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            return authentication.getName();
        }

        String preferredUsername =
                jwt.getClaimAsString(PREFERRED_USERNAME);

        if (preferredUsername != null
                && !preferredUsername.isBlank()) {
            return preferredUsername;
        }

        String email = jwt.getClaimAsString(EMAIL);

        if (email != null && !email.isBlank()) {
            return email;
        }

        return subject;
    }
}