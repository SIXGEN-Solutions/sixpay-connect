package com.sixpay.customer.management.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddCustomerBankAccountRequest(
        @NotBlank
        @Size(max = 100)
        String accountReference
) {
}
