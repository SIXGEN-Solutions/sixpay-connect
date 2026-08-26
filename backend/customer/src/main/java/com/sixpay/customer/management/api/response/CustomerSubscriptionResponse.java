package com.sixpay.customer.management.api.response;

import com.sixpay.customer.management.domain.model.CustomerSubscription;

import java.time.Instant;
import java.util.UUID;

public record CustomerSubscriptionResponse(
        UUID id,
        UUID customerId,
        UUID partnerId,
        UUID bankAccountId,
        String status,
        String statusReason,
        Instant createdAt,
        Instant activatedAt,
        Instant updatedAt,
        Instant closedAt
) {
    public static CustomerSubscriptionResponse from(
            CustomerSubscription subscription
    ) {
        return new CustomerSubscriptionResponse(
                subscription.id().value(),
                subscription.customerId().value(),
                subscription.partnerId(),
                subscription.bankAccountId().value(),
                subscription.status().name(),
                subscription.statusReason().orElse(null),
                subscription.createdAt(),
                subscription.activatedAt().orElse(null),
                subscription.updatedAt(),
                subscription.closedAt().orElse(null)
        );
    }
}
