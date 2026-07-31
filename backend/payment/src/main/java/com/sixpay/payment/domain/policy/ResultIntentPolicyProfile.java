package com.sixpay.payment.domain.policy;

import com.sixpay.payment.domain.model.PaymentStatus;

import java.util.Map;
import java.util.Objects;

public record ResultIntentPolicyProfile(
        PolicyProfileMetadata metadata,
        Map<PaymentStatus, ResultIntentDecision> resultByStatus
) {
    public ResultIntentPolicyProfile {
        metadata = Objects.requireNonNull(metadata, "Profile metadata");
        Objects.requireNonNull(resultByStatus, "Result-intent mapping");
        resultByStatus = Map.copyOf(resultByStatus);
    }

    public ResultIntentDecision decisionFor(PaymentStatus status) {
        return resultByStatus.getOrDefault(
                status,
                ResultIntentDecision.NONE
        );
    }
}
