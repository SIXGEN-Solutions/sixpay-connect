package com.sixpay.security.application.port.in;

import java.util.Set;
import java.util.UUID;

public record CreateSecurityUserCommand(
        UUID userId,
        String username,
        String email,
        Set<String> roles,
        Set<String> permissions,
        boolean localAuthenticationEnabled,
        String initialPassword,
        String actorSubject
) {
}
