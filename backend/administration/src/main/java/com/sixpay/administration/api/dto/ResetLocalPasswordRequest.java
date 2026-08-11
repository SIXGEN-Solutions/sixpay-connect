package com.sixpay.administration.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetLocalPasswordRequest(
        @NotBlank @Size(min = 12, max = 200) String newPassword
) {
}
