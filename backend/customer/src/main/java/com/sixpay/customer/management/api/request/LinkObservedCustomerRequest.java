package com.sixpay.customer.management.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record LinkObservedCustomerRequest(
        @NotNull UUID customerId,
        @NotBlank @Size(max = 500) String reason
) {
}
