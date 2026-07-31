package com.sixpay.payment.domain.service;

import com.sixpay.payment.domain.model.PaymentFailure;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.policy.*;

import java.time.Instant;
import java.util.Objects;

public final class PaymentResultIntentService {

    private final PaymentResultIntentPolicy policy;

    public PaymentResultIntentService() {
        this(new PaymentResultIntentPolicy());
    }

    public PaymentResultIntentService(
            PaymentResultIntentPolicy policy
    ) {
        this.policy = Objects.requireNonNull(
                policy,
                "Payment result-intent policy"
        );
    }

    public PolicyDecision<ResultIntentDecision> decide(
            PaymentResultContext context,
            PaymentStatus previousStatus,
            PaymentStatus resultingStatus,
            PaymentFailure failure,
            Instant availableAt,
            PaymentPolicyBundle profiles
    ) {
        Objects.requireNonNull(profiles, "Payment policy bundle");
        return policy.decide(
                context,
                previousStatus,
                resultingStatus,
                failure,
                availableAt,
                profiles.resultIntentPolicyProfile()
        );
    }
}
