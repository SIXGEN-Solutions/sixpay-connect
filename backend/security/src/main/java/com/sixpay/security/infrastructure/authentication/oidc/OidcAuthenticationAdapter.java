package com.sixpay.security.infrastructure.authentication.oidc;

import com.sixpay.security.application.exception.ExternalIdentityNotLinkedException;
import com.sixpay.security.application.exception.SixpayUserDisabledException;
import com.sixpay.security.application.port.output.ExternalIdentityResolver;
import com.sixpay.security.application.port.output.SecurityAuditPort;
import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.domain.administration.SecurityAuditEvent;
import com.sixpay.security.domain.administration.SecurityAuditEventType;
import com.sixpay.security.domain.authentication.ExternalIdentity;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class OidcAuthenticationAdapter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String PREFERRED_USERNAME = "preferred_username";
    private static final String EMAIL = "email";
    private static final String INVALID_TOKEN = "invalid_token";

    private final ExternalIdentityResolver externalIdentityResolver;
    private final SecurityAuditPort auditPort;

    public OidcAuthenticationAdapter(
            ExternalIdentityResolver externalIdentityResolver,
            SecurityAuditPort auditPort
    ) {
        this.externalIdentityResolver = Objects.requireNonNull(externalIdentityResolver);
        this.auditPort = Objects.requireNonNull(auditPort);
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Objects.requireNonNull(jwt, "JWT must not be null");

        ExternalIdentity externalIdentity = new ExternalIdentity(
                requiredClaim(jwt, JwtClaimNames.ISS),
                jwt.getSubject(),
                resolveUsername(jwt)
        );

        final AuthenticatedUser principal;
        try {
            principal = externalIdentityResolver.resolve(externalIdentity);
        } catch (ExternalIdentityNotLinkedException | SixpayUserDisabledException exception) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(INVALID_TOKEN),
                    "External identity cannot access SIXPAY"
            );
        }

        auditPort.record(new SecurityAuditEvent(
                SecurityAuditEventType.OIDC_LOGIN_SUCCESS,
                principal.subject(),
                parseUserId(principal.subject()),
                principal.username(),
                externalIdentity.issuer(),
                null,
                Instant.now()
        ));

        var authorities = principal.authorities()
                .stream()
                .map(SimpleGrantedAuthority::new)
                .toList();

        return new OidcAuthenticationToken(principal, authorities);
    }

    private String resolveUsername(Jwt jwt) {
        String preferredUsername = jwt.getClaimAsString(PREFERRED_USERNAME);
        if (preferredUsername != null && !preferredUsername.isBlank()) return preferredUsername;
        String email = jwt.getClaimAsString(EMAIL);
        if (email != null && !email.isBlank()) return email;
        return jwt.getSubject();
    }

    private String requiredClaim(Jwt jwt, String claimName) {
        String value = jwt.getClaimAsString(claimName);
        if (value == null || value.isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(INVALID_TOKEN),
                    "Required OIDC claim is missing"
            );
        }
        return value;
    }

    private static UUID parseUserId(String subject) {
        try { return UUID.fromString(subject); }
        catch (RuntimeException ignored) { return null; }
    }
}
