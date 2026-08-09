package com.sixpay.security.local.api;

import java.util.Set;

public final class LocalAuthResponses {

    private LocalAuthResponses() {
    }

    public record CurrentUserResponse(
            String subject,
            String username,
            Set<String> roles
    ) {
    }
}
