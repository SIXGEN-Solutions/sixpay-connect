package com.sixpay.security.configuration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyPropertiesTest {

    @Test
    void suppliesDa10DefaultsWhenConfigurationIsAbsent() {
        PasswordPolicyProperties properties =
                new PasswordPolicyProperties(
                        null,
                        null,
                        null,
                        null
                );

        assertThat(properties.toDomain().minLength()).isEqualTo(12);
        assertThat(properties.toDomain().maxLength()).isEqualTo(200);
        assertThat(properties.toDomain().historySize()).isEqualTo(5);
        assertThat(properties.toDomain().expirationDays()).isEqualTo(90);
    }

    @Test
    void exposesConfiguredPasswordPolicy() {
        PasswordPolicyProperties properties =
                new PasswordPolicyProperties(
                        14,
                        180,
                        8,
                        60
                );

        assertThat(properties.toDomain().minLength()).isEqualTo(14);
        assertThat(properties.toDomain().maxLength()).isEqualTo(180);
        assertThat(properties.toDomain().historySize()).isEqualTo(8);
        assertThat(properties.toDomain().expirationDays()).isEqualTo(60);
    }

    @Test
    void failsFastForInvalidConfiguration() {
        assertThatThrownBy(() ->
                new PasswordPolicyProperties(
                        20,
                        12,
                        5,
                        90
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maximum length");
    }
}
