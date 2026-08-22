package com.sixpay.customer.management.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record AddCustomerBankAccountRequest(
        @NotBlank @Size(max = 100) String bankingAccountReference,
        @NotBlank
        @Pattern(regexp = "^v1:[0-9a-f]{64}$")
        String accountBindingFingerprint,
        @NotBlank @Size(max = 100) String maskedAccountIdentifier,
        @NotBlank
        @Pattern(regexp = "^[A-Z]{3}$")
        String currency,
        @Size(max = 40) String accountType,
        Instant verifiedAt
) {
}
