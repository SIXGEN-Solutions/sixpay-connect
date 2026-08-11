package com.sixpay.security.api.dto;

import com.sixpay.security.domain.authentication.AuthenticationMethod;

import java.util.Set;

/**
 * Canonical backend SIXPAY session representation shared by Local and OIDC.
 */
public record AuthenticationSessionResponse(
        String subject,
        String username,
        Set<String> roles,
        Set<String> permissions,
        AuthenticationMethod authenticationMethod
) {
}
