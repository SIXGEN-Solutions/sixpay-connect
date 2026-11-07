package com.sixpay.security.api.controller;

import com.sixpay.security.api.error.LdapAuthenticationExceptionHandler;
import com.sixpay.security.application.exception.ExternalIdentityNotLinkedException;
import com.sixpay.security.application.exception.LdapAuthenticationFailedException;
import com.sixpay.security.application.exception.SixpayUserDisabledException;
import com.sixpay.security.application.port.input.AuthenticateLdapUserUseCase;
import com.sixpay.security.configuration.AuthenticationCapabilitiesProperties;
import com.sixpay.security.infrastructure.authentication.session.SpringSecuritySessionManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.beans.factory.annotation.Autowired;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = LdapAuthenticationController.class,
        properties = "sixpay.security.authentication.ldap.enabled=true"
)
@Import(LdapAuthenticationExceptionHandler.class)
@ContextConfiguration(
        classes = {
                LdapAuthenticationController.class,
                LdapAuthenticationExceptionHandler.class,
                LdapAuthenticationFailureIT.MvcTestConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
class LdapAuthenticationFailureIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticateLdapUserUseCase authenticateLdapUser;

    @MockitoBean
    private SpringSecuritySessionManager sessionManager;

    @MockitoBean
    private AuthenticationCapabilitiesProperties capabilities;

    @Test
    void invalidDirectoryCredentialsReturnGenericUnauthorized() throws Exception {
        assertGenericUnauthorized(new LdapAuthenticationFailedException());
    }

    @Test
    void unlinkedExternalIdentityReturnsSameGenericUnauthorized() throws Exception {
        assertGenericUnauthorized(new ExternalIdentityNotLinkedException());
    }

    @Test
    void disabledSixpayAccountReturnsSameGenericUnauthorized() throws Exception {
        assertGenericUnauthorized(new SixpayUserDisabledException());
    }

    private void assertGenericUnauthorized(RuntimeException failure) throws Exception {
        when(authenticateLdapUser.authenticate(any())).thenThrow(failure);

        mockMvc.perform(
                        post("/api/v1/auth/login/ldap")
                                .contentType("application/json")
                                .content("{\"username\":\"ldap-user\",\"password\":\"secret\"}")
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Authentication failed"))
                .andExpect(jsonPath("$.detail").value("Invalid credentials"));
    }

    /*
     * The security module is a library/auto-configuration module and has no
     * package-level @SpringBootConfiguration. Declare the MVC slice bootstrap
     * explicitly, following the existing PasswordChangeControllerIT pattern.
     */
    @Configuration(
            proxyBeanMethods = false
    )
    @Import({
            LdapAuthenticationController.class,
            LdapAuthenticationExceptionHandler.class
    })
    static class MvcTestConfiguration {
    }
}
