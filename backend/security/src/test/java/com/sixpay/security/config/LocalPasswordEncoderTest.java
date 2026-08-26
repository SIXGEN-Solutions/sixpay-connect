package com.sixpay.security.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class LocalPasswordEncoderTest {

    @Test
    void bcryptUsesStrengthTwelveAndNeverStoresRawPassword() {
        var encoder = new BCryptPasswordEncoder(12);
        var rawPassword = "Sixpay-Local-Password";

        var encoded = encoder.encode(rawPassword);

        assertThat(encoded).startsWith("$2");
        assertThat(encoded).isNotEqualTo(rawPassword);
        assertThat(encoder.matches(rawPassword, encoded)).isTrue();
        assertThat(encoder.matches("wrong-password", encoded)).isFalse();
    }
}
