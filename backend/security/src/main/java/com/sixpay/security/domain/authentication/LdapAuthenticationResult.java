package com.sixpay.security.domain.authentication;

import com.sixpay.common.validation.Preconditions;

public record LdapAuthenticationResult(
        AuthenticationIdentityType identityType,
        ExternalIdentity externalIdentity
) {

    public LdapAuthenticationResult {
        identityType = Preconditions.requireNonNull(
                identityType,
                "Authentication identity type must not be null"
        );
        externalIdentity = Preconditions.requireNonNull(
                externalIdentity,
                "External identity must not be null"
        );

        if (identityType != AuthenticationIdentityType.LDAP) {
            throw new IllegalArgumentException(
                    "LDAP authentication result must use LDAP identity type"
            );
        }
    }
}
