package com.sixpay.customer.management.api.response;

import com.sixpay.customer.management.domain.model.ObservedCustomerLink;

import java.time.Instant;
import java.util.UUID;

public record ObservedCustomerLinkResponse(
        UUID observedCustomerId,
        UUID customerId,
        String status,
        String linkedBy,
        String correlationId,
        String reason,
        Instant linkedAt
) {
    public static ObservedCustomerLinkResponse from(
            ObservedCustomerLink link
    ) {
        return new ObservedCustomerLinkResponse(
                link.observedCustomerId(),
                link.customerId().value(),
                link.status().name(),
                link.linkedBy(),
                link.linkCorrelationId(),
                link.linkReason(),
                link.linkedAt()
        );
    }
}
