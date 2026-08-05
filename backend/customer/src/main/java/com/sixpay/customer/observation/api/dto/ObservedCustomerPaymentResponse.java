package com.sixpay.customer.observation.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ObservedCustomerPaymentResponse(
        UUID paymentId,
        String paymentReference,
        String financialInstitutionCode,
        BigDecimal amount,
        String currency,
        String status,
        String reasonCode,
        Instant createdAt,
        Instant updatedAt
) {
}
