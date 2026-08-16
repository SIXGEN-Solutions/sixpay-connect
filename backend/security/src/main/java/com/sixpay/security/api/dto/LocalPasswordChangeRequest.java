package com.sixpay.security.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for an authenticated user's LOCAL password change.
 *
 * <p>Password length is intentionally enforced by the configurable
 * {@code PasswordPolicy}; DTO validation only rejects missing values.</p>
 */
public record LocalPasswordChangeRequest(
        @NotBlank String currentPassword,
        @NotBlank String newPassword
) {
}
