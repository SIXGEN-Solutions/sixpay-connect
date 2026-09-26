package com.sixpay.security.configuration;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticationCapabilitiesPropertiesTest {
    private static final int DEFAULT_MAXIMUM_FAILED_ATTEMPTS = 5;
    private static final Duration DEFAULT_LOCK_DURATION = Duration.ofMinutes(15);
    private static final int DEFAULT_BCRYPT_STRENGTH = 12;

    @Test
    void representsNoProviderForNonSecuredTechnicalRuntime() {
        AuthenticationCapabilitiesProperties properties =
                properties(false, false, false);

        assertCapabilities(properties, false, false, false, false, 0);
        assertThat(properties.anyProviderEnabled()).isFalse();
    }

    @Test void supportsLocalOnly() { assertCapabilities(properties(true,false,false), true,false,false,false,1); }
    @Test void supportsOidcOnly() { assertCapabilities(properties(false,true,false), false,true,false,false,1); }
    @Test void supportsLdapOnly() { assertCapabilities(properties(false,false,true), false,false,true,false,1); }
    @Test void supportsLocalAndOidc() { assertCapabilities(properties(true,true,false), true,true,false,true,2); }
    @Test void supportsLocalAndLdap() { assertCapabilities(properties(true,false,true), true,false,true,true,2); }
    @Test void supportsOidcAndLdap() { assertCapabilities(properties(false,true,true), false,true,true,true,2); }
    @Test void supportsAllProviders() { assertCapabilities(properties(true,true,true), true,true,true,true,3); }

    @Test
    void rejectsPlainLdapWhenCapabilityIsEnabled() {
        assertThatThrownBy(() -> new AuthenticationCapabilitiesProperties(
                local(false),
                oidc(false),
                new AuthenticationCapabilitiesProperties.Ldap(
                        true,
                        List.of("ldap://ad.example.test:389"),
                        "DC=example,DC=test",
                        "OU=Users",
                        null,null,null,null,null,
                        "CN=sixpay,OU=Service Accounts",
                        "runtime-secret",
                        null,null,null
                )
        )).isInstanceOf(IllegalArgumentException.class);
    }

    private static AuthenticationCapabilitiesProperties properties(boolean local, boolean oidc, boolean ldap) {
        return new AuthenticationCapabilitiesProperties(local(local), oidc(oidc), ldap ? ldapEnabled() : ldapDisabled());
    }

    private static AuthenticationCapabilitiesProperties.Local local(boolean enabled) {
        return new AuthenticationCapabilitiesProperties.Local(enabled, DEFAULT_MAXIMUM_FAILED_ATTEMPTS, DEFAULT_LOCK_DURATION, DEFAULT_BCRYPT_STRENGTH);
    }

    private static AuthenticationCapabilitiesProperties.Oidc oidc(boolean enabled) {
        return new AuthenticationCapabilitiesProperties.Oidc(enabled, enabled ? "sixpay" : null);
    }

    private static AuthenticationCapabilitiesProperties.Ldap ldapEnabled() {
        return new AuthenticationCapabilitiesProperties.Ldap(
                true,
                List.of("ldaps://ad.example.test:636"),
                "DC=example,DC=test",
                "OU=Users",
                null,null,null,null,null,
                "CN=sixpay,OU=Service Accounts",
                "runtime-secret",
                null,null,null
        );
    }

    private static AuthenticationCapabilitiesProperties.Ldap ldapDisabled() {
        return new AuthenticationCapabilitiesProperties.Ldap(
                false, List.of(), null,null,null,null,null,null,null,null,null,null,null,null
        );
    }

    private static void assertCapabilities(
            AuthenticationCapabilitiesProperties properties,
            boolean local, boolean oidc, boolean ldap, boolean hybrid, int count
    ) {
        assertThat(properties.localEnabled()).isEqualTo(local);
        assertThat(properties.oidcEnabled()).isEqualTo(oidc);
        assertThat(properties.ldapEnabled()).isEqualTo(ldap);
        assertThat(properties.hybridEnabled()).isEqualTo(hybrid);
        assertThat(properties.enabledProviderCount()).isEqualTo(count);
    }
}
