package com.sixpay.security.application.service;

import com.sixpay.security.application.port.input.AuthenticateLdapIdentityUseCase;
import com.sixpay.security.application.port.input.AuthenticateLdapUserUseCase;
import com.sixpay.security.application.port.input.LdapAuthenticationCommand;
import com.sixpay.security.application.port.output.ExternalIdentityResolver;
import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.domain.authentication.LdapAuthenticationResult;
import java.util.Objects;

public final class LdapCanonicalAuthenticationService
        implements AuthenticateLdapUserUseCase {

    private final AuthenticateLdapIdentityUseCase ldapAuthentication;
    private final ExternalIdentityResolver identityResolver;

    public LdapCanonicalAuthenticationService(
            AuthenticateLdapIdentityUseCase ldapAuthentication,
            ExternalIdentityResolver identityResolver
    ) {
        this.ldapAuthentication=Objects.requireNonNull(ldapAuthentication);
        this.identityResolver=Objects.requireNonNull(identityResolver);
    }

    @Override
    public AuthenticatedUser authenticate(LdapAuthenticationCommand command) {
        LdapAuthenticationResult identity =
                ldapAuthentication.authenticate(Objects.requireNonNull(command));
        return identityResolver.resolve(
                identity.identityType(),
                identity.externalIdentity()
        );
    }
}
