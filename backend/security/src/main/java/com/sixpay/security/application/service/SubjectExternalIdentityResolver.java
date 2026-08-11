package com.sixpay.security.application.service;

import com.sixpay.security.application.port.out.ExternalIdentityResolver;
import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.domain.authentication.ExternalIdentity;

import java.util.Objects;
import java.util.Set;

/**
 * DA-4 provider-neutral resolver.
 *
 * <p>Until DA-5 introduces authoritative identity linking, the external subject
 * remains the canonical authenticated subject already used by the existing
 * SIXPAY JWT path.</p>
 */
public final class SubjectExternalIdentityResolver
        implements ExternalIdentityResolver {

    @Override
    public AuthenticatedUser resolve(
            ExternalIdentity externalIdentity,
            Set<String> authorities
    ) {
        Objects.requireNonNull(
                externalIdentity,
                "External identity must not be null"
        );
        Objects.requireNonNull(
                authorities,
                "External authorities must not be null"
        );

        return new AuthenticatedUser(
                externalIdentity.subject(),
                externalIdentity.username(),
                authorities
        );
    }
}
