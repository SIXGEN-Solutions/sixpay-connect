package com.sixpay.security.application.port.input;

import java.util.UUID;

public record BootstrapLdapAdministratorCommand(
        UUID userId,
        String username,
        String email,
        String trustDomain,
        String stableSubject,
        String actorSubject
) {
}
