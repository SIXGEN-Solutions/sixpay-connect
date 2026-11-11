package com.sixpay.security.api.controller;

import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.configuration.AuthenticationCapabilitiesProperties;
import com.sixpay.security.domain.authentication.AuthenticationMethod;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticationSessionResponseTest {

    @Test
    void exposesLdapSessionAndBackendCapabilitiesWithoutProviderDerivedAuthorities() {
        AuthenticatedUser user = new AuthenticatedUser(
                "user-1",
                "alice",
                Set.of("ROLE_ADMIN", "SCOPE_payment.read")
        );

        AuthenticationCapabilitiesProperties capabilities =
                new AuthenticationCapabilitiesProperties(
                        new AuthenticationCapabilitiesProperties.Local(
                                true, 5, Duration.ofMinutes(15), 12
                        ),
                        new AuthenticationCapabilitiesProperties.Oidc(
                                false, null
                        ),
                        new AuthenticationCapabilitiesProperties.Ldap(
                                true,
                                List.of("ldaps://ad.example.test:636"),
                                "DC=example,DC=test",
                                "OU=Users",
                                null, null, null, null, null,
                                "CN=sixpay,OU=Service Accounts",
                                "runtime-secret",
                                null, null, null
                        )
                );

        var response = AuthenticationSessionController.toResponse(
                user,
                AuthenticationMethod.LDAP,
                capabilities
        );

        assertThat(response.authenticationMethod())
                .isEqualTo(AuthenticationMethod.LDAP);
        assertThat(response.roles()).containsExactly("ADMIN");
        assertThat(response.permissions()).containsExactly("SCOPE_payment.read");
        assertThat(response.passwordChangeRequired()).isFalse();
        assertThat(response.capabilities().localEnabled()).isTrue();
        assertThat(response.capabilities().oidcEnabled()).isFalse();
        assertThat(response.capabilities().ldapEnabled()).isTrue();
    }
}
