package com.sixpay.security.configuration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticationCapabilitiesPropertiesTest {

    @Test
    void defaultsBothCapabilitiesToDisabled() {
        AuthenticationCapabilitiesProperties properties =
                new AuthenticationCapabilitiesProperties(
                        null,
                        null
                );

        assertThat(properties.localEnabled()).isFalse();
        assertThat(properties.oidcEnabled()).isFalse();
        assertThat(properties.hybridEnabled()).isFalse();
    }

    @Test
    void supportsLocalOnly() {
        AuthenticationCapabilitiesProperties properties =
                new AuthenticationCapabilitiesProperties(
                        new AuthenticationCapabilitiesProperties.Local(
                                true
                        ),
                        new AuthenticationCapabilitiesProperties.Oidc(
                                false,
                                null
                        )
                );

        assertThat(properties.localEnabled()).isTrue();
        assertThat(properties.oidcEnabled()).isFalse();
        assertThat(properties.hybridEnabled()).isFalse();
    }

    @Test
    void supportsOidcOnly() {
        AuthenticationCapabilitiesProperties properties =
                new AuthenticationCapabilitiesProperties(
                        new AuthenticationCapabilitiesProperties.Local(
                                false
                        ),
                        new AuthenticationCapabilitiesProperties.Oidc(
                                true,
                                "sixpay"
                        )
                );

        assertThat(properties.localEnabled()).isFalse();
        assertThat(properties.oidcEnabled()).isTrue();
        assertThat(properties.hybridEnabled()).isFalse();
    }

    @Test
    void supportsHybridLocalAndOidc() {
        AuthenticationCapabilitiesProperties properties =
                new AuthenticationCapabilitiesProperties(
                        new AuthenticationCapabilitiesProperties.Local(
                                true
                        ),
                        new AuthenticationCapabilitiesProperties.Oidc(
                                true,
                                "sixpay"
                        )
                );

        assertThat(properties.localEnabled()).isTrue();
        assertThat(properties.oidcEnabled()).isTrue();
        assertThat(properties.hybridEnabled()).isTrue();
    }
}
