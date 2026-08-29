package com.sixpay.security.application.service;

import com.sixpay.security.application.exception.ExternalIdentityNotLinkedException;
import com.sixpay.security.application.exception.SixpayUserDisabledException;
import com.sixpay.security.application.port.output.ExternalIdentityResolver;
import com.sixpay.security.application.port.output.FindLinkedIdentityPort;
import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.domain.authentication.AuthenticationIdentityType;
import com.sixpay.security.domain.authentication.ExternalIdentity;
import com.sixpay.security.domain.authentication.LinkedUserIdentity;

import java.util.Objects;

/**
 * Resolves OIDC identity to a canonical SIXPAY user and loads authorization
 * exclusively from that SIXPAY user account.
 */
public class LinkedExternalIdentityResolver
        implements ExternalIdentityResolver {

    private final FindLinkedIdentityPort findLinkedIdentityPort;

    public LinkedExternalIdentityResolver(
            FindLinkedIdentityPort findLinkedIdentityPort
    ) {
        this.findLinkedIdentityPort =
                Objects.requireNonNull(findLinkedIdentityPort);
    }

    @Override
    public AuthenticatedUser resolve(
            ExternalIdentity externalIdentity
    ) {
        Objects.requireNonNull(
                externalIdentity,
                "External identity must not be null"
        );

        LinkedUserIdentity linkedIdentity =
                findLinkedIdentityPort.findLinkedIdentity(
                                AuthenticationIdentityType.OIDC,
                                externalIdentity.issuer(),
                                externalIdentity.subject()
                        )
                        .orElseThrow(
                                ExternalIdentityNotLinkedException::new
                        );

        if (!linkedIdentity.userAccount().active()) {
            throw new SixpayUserDisabledException();
        }

        return new AuthenticatedUser(
                linkedIdentity.userAccount().canonicalSubject(),
                linkedIdentity.userAccount().username(),
                linkedIdentity.userAccount().authorities()
        );
    }
}
