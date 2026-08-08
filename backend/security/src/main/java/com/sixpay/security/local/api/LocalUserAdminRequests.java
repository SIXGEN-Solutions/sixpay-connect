package com.sixpay.security.local.api;

import com.sixpay.security.local.LocalRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public final class LocalUserAdminRequests {

    private LocalUserAdminRequests() {
    }

    public record CreateLocalUserRequest(
            @NotBlank
            @Size(min = 3, max = 100)
            String username,

            @NotBlank
            @Size(min = 12, max = 200)
            String password,

            @NotBlank
            @Size(max = 150)
            String subject,

            @NotEmpty
            Set<LocalRole> roles
    ) {
    }
}
