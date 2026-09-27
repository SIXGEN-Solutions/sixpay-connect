package com.sixpay.security.api.controller;

import com.sixpay.security.api.dto.AuthenticationSessionResponse;
import com.sixpay.security.application.port.input.AuthenticateLdapUserUseCase;
import com.sixpay.security.application.port.input.LdapAuthenticationCommand;
import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.configuration.AuthenticationCapabilitiesProperties;
import com.sixpay.security.domain.authentication.AuthenticationMethod;
import com.sixpay.security.infrastructure.authentication.session.SpringSecuritySessionManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LdapAuthenticationControllerTest {

    @Test
    void establishesCanonicalLdapSession() {
        AuthenticateLdapUserUseCase authenticateLdapUser =
                mock(AuthenticateLdapUserUseCase.class);
        SpringSecuritySessionManager sessionManager =
                mock(SpringSecuritySessionManager.class);
        HttpServletRequest servletRequest =
                mock(HttpServletRequest.class);
        HttpServletResponse servletResponse =
                mock(HttpServletResponse.class);

        AuthenticatedUser user =
                new AuthenticatedUser(
                        "11111111-1111-4111-8111-111111111111",
                        "ldap-user",
                        Set.of(
                                "ROLE_ADMIN",
                                "customer.read"
                        ),
                        false
                );

        when(authenticateLdapUser.authenticate(any(LdapAuthenticationCommand.class)))
                .thenReturn(user);

        AuthenticationCapabilitiesProperties capabilities =
                new AuthenticationCapabilitiesProperties(
                        new AuthenticationCapabilitiesProperties.Local(
                                false,
                                5,
                                Duration.ofMinutes(15),
                                12
                        ),
                        new AuthenticationCapabilitiesProperties.Oidc(false, null),
                        new AuthenticationCapabilitiesProperties.Ldap(
                                true,
                                List.of("ldaps://ldap.example.test"),
                                "dc=example,dc=test",
                                "ou=users",
                                "(sAMAccountName={0})",
                                "sAMAccountName",
                                "sAMAccountName",
                                "objectGUID",
                                "regionale-ldap",
                                "cn=service-account",
                                "secret-for-test-only",
                                Duration.ofSeconds(3),
                                Duration.ofSeconds(5),
                                Duration.ofSeconds(10)
                        )
                );

        LdapAuthenticationController controller =
                new LdapAuthenticationController(
                        authenticateLdapUser,
                        sessionManager,
                        capabilities
                );

        AuthenticationSessionResponse response =
                controller.login(
                                new com.sixpay.security.api.dto.LdapLoginRequest(
                                        "ldap-user",
                                        "password"
                                ),
                                servletRequest,
                                servletResponse
                        )
                        .getBody();

        verify(authenticateLdapUser)
                .authenticate(new LdapAuthenticationCommand("ldap-user", "password"));
        verify(sessionManager)
                .startSession(
                        user,
                        AuthenticationMethod.LDAP,
                        servletRequest,
                        servletResponse
                );

        assertEquals(AuthenticationMethod.LDAP, response.authenticationMethod());
        assertEquals("ldap-user", response.username());
        assertEquals(Set.of("ADMIN"), response.roles());
        assertEquals(Set.of("customer.read"), response.permissions());
        assertEquals(true, response.capabilities().ldapEnabled());
        assertEquals(false, response.passwordChangeRequired());
    }
}
