package com.sixpay.security.application.port.input;

import com.sixpay.common.validation.Preconditions;

public record LdapAuthenticationCommand(
        String username,
        String password
) {

    public LdapAuthenticationCommand {
        username = Preconditions.requireNonBlank(
                username,
                "LDAP username must not be blank"
        );
        password = Preconditions.requireNonBlank(
                password,
                "LDAP password must not be blank"
        );
    }
}
