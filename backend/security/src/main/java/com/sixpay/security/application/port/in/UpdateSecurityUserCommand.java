package com.sixpay.security.application.port.in;

import java.util.Set;
import java.util.UUID;

public record UpdateSecurityUserCommand(
        UUID userId,
        String username,
        String email,
        Set<String> roles,
        Set<String> permissions,
        String actorSubject
) {
}
