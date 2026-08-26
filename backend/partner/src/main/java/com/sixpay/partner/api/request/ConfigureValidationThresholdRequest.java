package com.sixpay.partner.api.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ConfigureValidationThresholdRequest(
        @NotBlank @Size(min = 3, max = 3) @Pattern(regexp = "[A-Za-z]{3}") String currency,
        @NotNull @DecimalMin(value = "0.0001") BigDecimal amount,
        @Min(1) @Max(10) int validationLevels
) {
}
