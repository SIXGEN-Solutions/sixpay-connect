package com.sixpay.customer.management.application.port.output;

import java.util.UUID;

public interface PartnerSubscriptionEligibilityPort {

    PartnerEligibility check(UUID partnerId);

    record PartnerEligibility(
            boolean exists,
            boolean active
    ) {
    }
}
