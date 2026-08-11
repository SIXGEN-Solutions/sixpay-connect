package com.sixpay.security.api.dto;

import java.util.Set;

/**
 * Mechanism-neutral authenticated SIXPAY session representation.
 */
public record AuthenticationSessionResponse(
        String subject,
        String username,
        Set<String> roles,
        Set<String> permissions
) {
}
