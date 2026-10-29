package com.sixpay.security.api.controller;

import com.sixpay.security.api.dto.AuthenticationSessionResponse;
import com.sixpay.security.api.dto.LdapLoginRequest;
import com.sixpay.security.application.port.input.AuthenticateLdapUserUseCase;
import com.sixpay.security.application.port.input.LdapAuthenticationCommand;
import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.configuration.AuthenticationCapabilitiesProperties;
import com.sixpay.security.domain.authentication.AuthenticationMethod;
import com.sixpay.security.infrastructure.authentication.session.SpringSecuritySessionManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * LDAP credential boundary.
 *
 * <p>LDAP proves the external identity. SIXPAY resolves the existing durable
 * LDAP identity link, loads SIXPAY-owned roles/permissions and establishes the
 * same backend session model used by LOCAL and OIDC.</p>
 */
@RestController
@RequestMapping("/api/v1/auth")
@ConditionalOnProperty(
        prefix = "sixpay.security.authentication.ldap",
        name = "enabled",
        havingValue = "true"
)
public final class LdapAuthenticationController {

    private final AuthenticateLdapUserUseCase authenticateLdapUser;
    private final SpringSecuritySessionManager sessionManager;
    private final AuthenticationCapabilitiesProperties capabilities;

    public LdapAuthenticationController(
            AuthenticateLdapUserUseCase authenticateLdapUser,
            SpringSecuritySessionManager sessionManager,
            AuthenticationCapabilitiesProperties capabilities
    ) {
        this.authenticateLdapUser = Objects.requireNonNull(authenticateLdapUser);
        this.sessionManager = Objects.requireNonNull(sessionManager);
        this.capabilities = Objects.requireNonNull(capabilities);
    }

    @PostMapping("/login/ldap")
    public ResponseEntity<AuthenticationSessionResponse> login(
            @Valid @RequestBody LdapLoginRequest requestBody,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        AuthenticatedUser authenticatedUser =
                authenticateLdapUser.authenticate(
                        new LdapAuthenticationCommand(
                                requestBody.username(),
                                requestBody.password()
                        )
                );

        sessionManager.startSession(
                authenticatedUser,
                AuthenticationMethod.LDAP,
                request,
                response
        );

        return ResponseEntity.ok(
                AuthenticationSessionController.toResponse(
                        authenticatedUser,
                        AuthenticationMethod.LDAP,
                        capabilities
                )
        );
    }
}
