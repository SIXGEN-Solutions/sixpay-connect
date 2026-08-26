package com.sixpay.security.api.dto;

import java.util.Set;

public record LocalSessionResponse(
        String subject,
        String username,
        Set<String> roles
) {
}
