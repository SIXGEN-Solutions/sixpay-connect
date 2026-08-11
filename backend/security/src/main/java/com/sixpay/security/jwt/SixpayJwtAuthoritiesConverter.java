package com.sixpay.security.jwt;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.Set;

/**
 * Legacy compatibility converter.
 *
 * @deprecated DA-6 makes SIXPAY the sole owner of business authorization.
 * JWT scopes, roles and IdP groups must not become business authorities.
 * The active OIDC authentication path no longer uses this converter.
 */
@Deprecated(forRemoval = true)
public final class SixpayJwtAuthoritiesConverter
        implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        return Set.of();
    }
}
