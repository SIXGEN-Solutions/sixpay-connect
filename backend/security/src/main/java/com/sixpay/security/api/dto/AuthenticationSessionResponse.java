package com.sixpay.security.api.dto;

import com.sixpay.security.domain.authentication.AuthenticationMethod;

import java.util.Set;

/**
 * Canonical backend SIXPAY session representation shared by Local and OIDC.
 *
 * <p>{@code passwordChangeRequired} is always false for OIDC because password
 * lifecycle is owned by the IdP.</p>
 */
public record AuthenticationSessionResponse(
        boolean authenticated,
        String subject,
        String username,
        Set<String> roles,
        Set<String> permissions,
        AuthenticationMethod authenticationMethod,
        boolean passwordChangeRequired
) {
}
