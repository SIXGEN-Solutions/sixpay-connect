package com.sixpay.security.application.service;

import com.sixpay.security.application.exception.ExternalIdentityNotLinkedException;
import com.sixpay.security.application.exception.SixpayUserDisabledException;
import com.sixpay.security.application.port.out.ExternalIdentityResolver;
import com.sixpay.security.application.port.out.FindLinkedIdentityPort;
import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.domain.authentication.AuthenticationIdentityType;
import com.sixpay.security.domain.authentication.ExternalIdentity;
import com.sixpay.security.domain.authentication.LinkedUserIdentity;

import java.util.Objects;
import java.util.Set;

/**
 * Conservative OIDC identity resolver.
 *
 * <p>An externally authenticated subject is accepted only when a pre-existing
 * OIDC identity link exists. Email and username claims are never used to
 * auto-provision or auto-link an account.</p>
 */
public class LinkedExternalIdentityResolver
        implements ExternalIdentityResolver {

    private final FindLinkedIdentityPort findLinkedIdentityPort;

    public LinkedExternalIdentityResolver(
            FindLinkedIdentityPort findLinkedIdentityPort
    ) {
        this.findLinkedIdentityPort = Objects.requireNonNull(findLinkedIdentityPort);
    }

    @Override
    public AuthenticatedUser resolve(
            ExternalIdentity externalIdentity,
            Set<String> authorities
    ) {
        Objects.requireNonNull(externalIdentity, "External identity must not be null");
        Objects.requireNonNull(authorities, "External authorities must not be null");

        LinkedUserIdentity linkedIdentity =
                findLinkedIdentityPort.findLinkedIdentity(
                                AuthenticationIdentityType.OIDC,
                                externalIdentity.issuer(),
                                externalIdentity.subject()
                        )
                        .orElseThrow(ExternalIdentityNotLinkedException::new);

        if (!linkedIdentity.userAccount().active()) {
            throw new SixpayUserDisabledException();
        }

        return new AuthenticatedUser(
                linkedIdentity.userAccount().canonicalSubject(),
                linkedIdentity.userAccount().username(),
                authorities
        );
    }
}
