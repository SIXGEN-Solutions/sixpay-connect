package com.sixpay.administration.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UpdateSecurityUserRequest(
        @NotBlank @Size(max = 150) String username,
        @Email @Size(max = 320) String email,
        Set<@NotBlank @Size(max = 100) String> roles,
        Set<@NotBlank @Size(max = 150) String> permissions
) {
}
