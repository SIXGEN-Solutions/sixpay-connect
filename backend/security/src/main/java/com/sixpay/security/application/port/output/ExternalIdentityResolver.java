package com.sixpay.security.application.port.output;

import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.domain.authentication.AuthenticationIdentityType;
import com.sixpay.security.domain.authentication.ExternalIdentity;

@FunctionalInterface
public interface ExternalIdentityResolver {

    AuthenticatedUser resolve(ExternalIdentity externalIdentity);

    default AuthenticatedUser resolve(
            AuthenticationIdentityType identityType,
            ExternalIdentity externalIdentity
    ) {
        if (identityType != AuthenticationIdentityType.OIDC) {
            throw new IllegalArgumentException(
                    "Explicit external identity type is not supported"
            );
        }
        return resolve(externalIdentity);
    }
}
