package com.sixpay.security.infrastructure.authentication.oidc;

import com.sixpay.security.application.exception.ExternalIdentityNotLinkedException;
import com.sixpay.security.application.exception.SixpayUserDisabledException;
import com.sixpay.security.application.port.out.ExternalIdentityResolver;
import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.domain.authentication.ExternalIdentity;
import com.sixpay.security.jwt.SixpayJwtAuthoritiesConverter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

public final class OidcAuthenticationAdapter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String PREFERRED_USERNAME = "preferred_username";
    private static final String EMAIL = "email";
    private static final String INVALID_TOKEN = "invalid_token";

    private final SixpayJwtAuthoritiesConverter authoritiesConverter;
    private final ExternalIdentityResolver externalIdentityResolver;

    public OidcAuthenticationAdapter(
            SixpayJwtAuthoritiesConverter authoritiesConverter,
            ExternalIdentityResolver externalIdentityResolver
    ) {
        this.authoritiesConverter = Objects.requireNonNull(authoritiesConverter);
        this.externalIdentityResolver = Objects.requireNonNull(externalIdentityResolver);
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Objects.requireNonNull(jwt, "JWT must not be null");

        Collection<GrantedAuthority> convertedAuthorities =
                authoritiesConverter.convert(jwt);

        Set<GrantedAuthority> authorities =
                convertedAuthorities == null
                        ? Set.of()
                        : Set.copyOf(convertedAuthorities);

        Set<String> authorityNames =
                authorities.stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(java.util.stream.Collectors.toUnmodifiableSet());

        ExternalIdentity externalIdentity =
                new ExternalIdentity(
                        requiredClaim(jwt, JwtClaimNames.ISS),
                        jwt.getSubject(),
                        resolveUsername(jwt)
                );

        final AuthenticatedUser principal;
        try {
            principal = externalIdentityResolver.resolve(
                    externalIdentity,
                    authorityNames
            );
        } catch (ExternalIdentityNotLinkedException
                 | SixpayUserDisabledException exception) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(INVALID_TOKEN),
                    "External identity cannot access SIXPAY"
            );
        }

        return new OidcAuthenticationToken(
                principal,
                authorities
        );
    }

    private String resolveUsername(Jwt jwt) {
        String preferredUsername =
                jwt.getClaimAsString(PREFERRED_USERNAME);

        if (preferredUsername != null && !preferredUsername.isBlank()) {
            return preferredUsername;
        }

        String email = jwt.getClaimAsString(EMAIL);

        if (email != null && !email.isBlank()) {
            return email;
        }

        return jwt.getSubject();
    }

    private String requiredClaim(
            Jwt jwt,
            String claimName
    ) {
        String value = jwt.getClaimAsString(claimName);

        if (value == null || value.isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(INVALID_TOKEN),
                    "Required OIDC claim is missing"
            );
        }

        return value;
    }
}
