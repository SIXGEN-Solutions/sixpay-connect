package com.sixpay.security.jwt;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority
        .SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource
        .authentication.JwtGrantedAuthoritiesConverter;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Converts OAuth2 scopes and SIXPAY roles contained in a JWT
 * into Spring Security authorities.
 */
public final class SixpayJwtAuthoritiesConverter
        implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String ROLES_CLAIM = "roles";
    private static final String ROLE_PREFIX = "ROLE_";

    private final JwtGrantedAuthoritiesConverter scopeConverter =
            new JwtGrantedAuthoritiesConverter();

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> authorities =
                new LinkedHashSet<>();

        Collection<GrantedAuthority> scopeAuthorities =
                scopeConverter.convert(jwt);

        if (scopeAuthorities != null) {
            authorities.addAll(scopeAuthorities);
        }

        extractRoles(jwt).stream()
                .map(this::normalizeRole)
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);

        return Set.copyOf(authorities);
    }

    private Set<String> extractRoles(Jwt jwt) {
        Object rolesClaim = jwt.getClaim(ROLES_CLAIM);

        if (rolesClaim instanceof Collection<?> roles) {
            Set<String> extractedRoles =
                    new LinkedHashSet<>();

            roles.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .filter(role -> !role.isBlank())
                    .forEach(extractedRoles::add);

            return extractedRoles;
        }

        if (rolesClaim instanceof String roles
                && !roles.isBlank()) {
            return Set.of(roles.split("\\s+"));
        }

        return Set.of();
    }

    private String normalizeRole(String role) {
        String normalizedRole = role
                .trim()
                .toUpperCase(Locale.ROOT);

        if (normalizedRole.startsWith(ROLE_PREFIX)) {
            return normalizedRole;
        }

        return ROLE_PREFIX + normalizedRole;
    }
}