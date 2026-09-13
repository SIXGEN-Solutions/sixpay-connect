package com.sixpay.administration.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DynamicSettingUpdateRequest(
        @NotBlank @Size(max = 2048) String value,
        @NotBlank @Size(max = 1024) String reason
) {
}
