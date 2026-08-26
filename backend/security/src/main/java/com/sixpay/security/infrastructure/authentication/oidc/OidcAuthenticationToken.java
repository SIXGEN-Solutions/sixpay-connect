package com.sixpay.security.infrastructure.authentication.oidc;

import com.sixpay.security.authentication.AuthenticatedUser;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Objects;

/**
 * Spring Security authentication token whose principal is already the
 * canonical SIXPAY authenticated identity.
 */
public final class OidcAuthenticationToken
        extends AbstractAuthenticationToken {

    private final AuthenticatedUser principal;

    public OidcAuthenticationToken(
            AuthenticatedUser principal,
            Collection<? extends GrantedAuthority> authorities
    ) {
        super(authorities);
        this.principal = Objects.requireNonNull(
                principal,
                "OIDC principal must not be null"
        );
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public AuthenticatedUser getPrincipal() {
        return principal;
    }

    @Override
    public String getName() {
        return principal.subject();
    }
}
