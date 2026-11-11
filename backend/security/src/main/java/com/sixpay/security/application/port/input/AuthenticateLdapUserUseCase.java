package com.sixpay.security.application.port.input;

import com.sixpay.security.authentication.AuthenticatedUser;

@FunctionalInterface
public interface AuthenticateLdapUserUseCase {
    AuthenticatedUser authenticate(LdapAuthenticationCommand command);
}
