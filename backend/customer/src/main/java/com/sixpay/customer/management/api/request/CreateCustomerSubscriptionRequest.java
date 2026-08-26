package com.sixpay.customer.management.api.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateCustomerSubscriptionRequest(
        @NotNull UUID customerId,
        @NotNull UUID partnerId,
        @NotNull UUID bankAccountId
) {
}
