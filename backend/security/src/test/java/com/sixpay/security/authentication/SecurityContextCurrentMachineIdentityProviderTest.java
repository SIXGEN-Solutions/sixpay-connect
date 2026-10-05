package com.sixpay.security.authentication;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityContextCurrentMachineIdentityProviderTest {

    private final SecurityContextCurrentMachineIdentityProvider provider =
            new SecurityContextCurrentMachineIdentityProvider();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void exposesAuthenticatedTechnicalSubject() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        "partner-client-001",
                        "N/A",
                        List.of()
                )
        );

        assertThat(provider.requireCurrentMachineIdentity().subject())
                .isEqualTo("partner-client-001");
    }

    @Test
    void doesNotTreatHumanSixpayUserAsMachineCaller() {
        AuthenticatedUser user = new AuthenticatedUser(
                "user-subject",
                "userseed",
                Set.of("ROLE_PARTNER")
        );

        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        user,
                        "N/A",
                        List.of()
                )
        );

        assertThat(provider.currentMachineIdentity()).isEmpty();
    }
}
