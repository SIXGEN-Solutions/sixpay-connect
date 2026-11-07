package com.sixpay.security.application.exception;

public class LdapAuthenticationFailedException
        extends RuntimeException {

    public LdapAuthenticationFailedException() {
        super("LDAP authentication failed");
    }

    public LdapAuthenticationFailedException(
            Throwable cause
    ) {
        super("LDAP authentication failed", cause);
    }
}
