package com.sixpay.security.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LocalLoginRequest(
        @NotBlank
        @Size(max = 150)
        String username,

        @NotBlank
        @Size(max = 1024)
        String password
) {
}
