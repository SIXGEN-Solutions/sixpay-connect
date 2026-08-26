package com.sixpay.customer.management.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCustomerRequest(
        @NotBlank @Size(max = 200) String legalName,
        @Email @Size(max = 254) String email,
        @Size(max = 32) String phoneNumber
) {
}
