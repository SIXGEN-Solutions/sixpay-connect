package com.sixpay.customer.management.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BankingCustomerPreviewRequest(
        @NotBlank @Size(max = 50) String financialInstitutionCode,
        @NotBlank @Size(max = 50) String customerNumber
) {
}
