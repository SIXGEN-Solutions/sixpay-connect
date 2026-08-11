package com.sixpay.security.configuration;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticationCapabilitiesPropertiesTest {

    private static final int DEFAULT_MAXIMUM_FAILED_ATTEMPTS = 5;
    private static final Duration DEFAULT_LOCK_DURATION =
            Duration.ofMinutes(15);
    private static final int DEFAULT_BCRYPT_STRENGTH = 12;

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
                        local(true),
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
                        local(false),
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
                        local(true),
                        new AuthenticationCapabilitiesProperties.Oidc(
                                true,
                                "sixpay"
                        )
                );

        assertThat(properties.localEnabled()).isTrue();
        assertThat(properties.oidcEnabled()).isTrue();
        assertThat(properties.hybridEnabled()).isTrue();
    }

    private static AuthenticationCapabilitiesProperties.Local local(
            boolean enabled
    ) {
        return new AuthenticationCapabilitiesProperties.Local(
                enabled,
                DEFAULT_MAXIMUM_FAILED_ATTEMPTS,
                DEFAULT_LOCK_DURATION,
                DEFAULT_BCRYPT_STRENGTH
        );
    }
}