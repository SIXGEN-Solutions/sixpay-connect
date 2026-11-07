package com.sixpay.security.infrastructure.authentication.ldap;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LdapOwnershipArchitectureTest {

    @Test
    void ldapImplementationRemainsOwnedBySecurityInfrastructure() {
        assertThat(
                ActiveDirectoryDirectoryUserLookupAdapter.class
                        .getPackageName()
        ).startsWith(
                "com.sixpay.security.infrastructure.authentication.ldap"
        );

        assertThat(
                ActiveDirectoryLdapAuthenticationAdapter.class
                        .getPackageName()
        ).startsWith(
                "com.sixpay.security.infrastructure.authentication.ldap"
        );
    }
}
