package com.sixpay.security.domain.authentication;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {

    private final PasswordPolicy policy =
            new PasswordPolicy(12, 200, 5, 90);

    @Test
    void acceptsPasswordAtConfiguredLengthBoundaries() {
        assertThatCode(() -> policy.validate("a".repeat(12)))
                .doesNotThrowAnyException();

        assertThatCode(() -> policy.validate("a".repeat(200)))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsPasswordShorterThanConfiguredMinimum() {
        assertThatThrownBy(() -> policy.validate("a".repeat(11)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Password must contain at least 12 characters");
    }

    @Test
    void rejectsPasswordLongerThanConfiguredMaximum() {
        assertThatThrownBy(() -> policy.validate("a".repeat(201)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Password must contain at most 200 characters");
    }

    @Test
    void rejectsNullPassword() {
        assertThatThrownBy(() -> policy.validate(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Password must not be null");
    }

    @Test
    void rejectsInvalidPolicyDefinition() {
        assertThatThrownBy(() -> new PasswordPolicy(0, 200, 5, 90))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new PasswordPolicy(12, 11, 5, 90))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new PasswordPolicy(12, 200, -1, 90))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new PasswordPolicy(12, 200, 5, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
