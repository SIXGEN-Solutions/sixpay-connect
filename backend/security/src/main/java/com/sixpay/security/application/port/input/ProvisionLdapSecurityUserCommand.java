package com.sixpay.security.application.port.input;

import java.util.Set;

public record ProvisionLdapSecurityUserCommand(
        String directoryUsername,
        Set<String> roles,
        Set<String> permissions,
        String actorSubject
) {
}
