package com.sixpay.security.api.dto;

import jakarta.validation.constraints.NotBlank;

public record LdapLoginRequest(
        @NotBlank String username,
        @NotBlank String password
) {
}
