package com.sixpay.customer.observation.api.dto;

import java.time.Instant;
import java.util.UUID;

public record ObservedCustomerSummaryResponse(
        UUID observedCustomerId,
        MaskedIdentifierResponse niu,
        String legalName,
        MaskedIdentifierResponse phone,
        MaskedIdentifierResponse email,
        Instant firstObservedAt,
        Instant lastObservedAt,
        long totalPayments,
        long successfulPayments,
        long failedPayments,
        String lastPaymentStatus,
        String lastFailureReasonCode,
        Instant projectionUpdatedAt,
        long projectionVersion
) {
}
