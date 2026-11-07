package com.sixpay.security.application.port.input;

import com.sixpay.security.domain.authentication.LdapAuthenticationResult;

@FunctionalInterface
public interface AuthenticateLdapIdentityUseCase {

    LdapAuthenticationResult authenticate(
            LdapAuthenticationCommand command
    );
}
