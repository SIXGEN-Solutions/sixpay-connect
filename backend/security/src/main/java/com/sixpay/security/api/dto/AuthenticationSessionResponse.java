package com.sixpay.security.api.dto;

import com.sixpay.security.domain.authentication.AuthenticationMethod;

import java.util.Set;

/**
 * Canonical backend SIXPAY session representation shared by LOCAL, OIDC and LDAP.
 *
 * <p>{@code passwordChangeRequired} is LOCAL-only. Password lifecycle for
 * OIDC and LDAP remains owned by the external identity provider/directory.</p>
 */
public record AuthenticationSessionResponse(
        boolean authenticated,
        String subject,
        String username,
        Set<String> roles,
        Set<String> permissions,
        AuthenticationMethod authenticationMethod,
        boolean passwordChangeRequired,
        AuthenticationCapabilitiesResponse capabilities
) {
}
