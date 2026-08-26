package com.sixpay.customer.management.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BankingCustomerPreviewRequest(
        @NotBlank @Size(max = 50) String financialInstitutionCode,
        @Size(max = 100) String niu,
        @Size(max = 100) String customerNumber,
        @NotBlank @Size(max = 100) String accountReference
) {
}
