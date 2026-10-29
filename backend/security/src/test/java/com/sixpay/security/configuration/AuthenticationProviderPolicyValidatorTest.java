package com.sixpay.security.configuration;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticationProviderPolicyValidatorTest {

    @Test
    void rejectsZeroProviderWhenPolicyRequiresOne() {
        assertThatThrownBy(() -> new AuthenticationProviderPolicyValidator(
                capabilities(false, false),
                new AuthenticationProviderPolicyProperties(true)
        )).isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("At least one authentication provider");
    }

    @Test
    void allowsZeroProviderForTechnicalRuntime() {
        assertThatNoException().isThrownBy(() -> new AuthenticationProviderPolicyValidator(
                capabilities(false, false),
                new AuthenticationProviderPolicyProperties(false)
        ));
    }

    @Test
    void allowsSecuredRuntimeWithProvider() {
        assertThatNoException().isThrownBy(() -> new AuthenticationProviderPolicyValidator(
                capabilities(true, false),
                new AuthenticationProviderPolicyProperties(true)
        ));
    }

    private static AuthenticationCapabilitiesProperties capabilities(
            boolean local, boolean oidc
    ) {
        return new AuthenticationCapabilitiesProperties(
                new AuthenticationCapabilitiesProperties.Local(
                        local, 5, Duration.ofMinutes(15), 12
                ),
                new AuthenticationCapabilitiesProperties.Oidc(
                        oidc, oidc ? "sixpay" : null
                ),
                AuthenticationCapabilitiesProperties.Ldap.disabled()
        );
    }
}
