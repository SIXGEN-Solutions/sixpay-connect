package com.sixpay.administration.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DynamicSettingRollbackRequest(
        @Min(1) long targetVersion,
        @NotBlank @Size(max = 1024) String reason
) {
}
