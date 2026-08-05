package com.sixpay.customer.observation.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ObservedCustomerDetailResponse(
        UUID observedCustomerId,
        MaskedIdentifierResponse niu,
        String legalName,
        MaskedIdentifierResponse phone,
        MaskedIdentifierResponse email,
        List<InstitutionObservationResponse> institutions,
        Instant firstObservedAt,
        Instant lastObservedAt,
        long totalPayments,
        long successfulPayments,
        long failedPayments,
        String lastPaymentStatus,
        String lastFailureReasonCode,
        Instant projectionUpdatedAt,
        long projectionVersion,
        String sourceEventWatermark
) {
}
