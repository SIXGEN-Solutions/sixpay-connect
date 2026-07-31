package com.sixpay.payment.domain.policy;

import com.sixpay.payment.domain.model.PaymentFailure;
import com.sixpay.payment.domain.model.PaymentStatus;

import java.time.Instant;
import java.util.Objects;

public final class PaymentResultIntentPolicy {

    public PolicyDecision<ResultIntentDecision> decide(
            PaymentResultContext context,
            PaymentStatus previousStatus,
            PaymentStatus resultingStatus,
            PaymentFailure failure,
            Instant availableAt,
            ResultIntentPolicyProfile profile
    ) {
        Objects.requireNonNull(context, "Payment result context");
        Objects.requireNonNull(previousStatus, "Previous status");
        Objects.requireNonNull(resultingStatus, "Resulting status");
        Objects.requireNonNull(availableAt, "Result available instant");
        Objects.requireNonNull(profile, "Result-intent profile");

        if (!profile.metadata().isEffectiveAt(availableAt)) {
            return PolicyDecision.withProfile(
                    ResultIntentDecision.NONE,
                    "PROFILE_NOT_EFFECTIVE",
                    profile.metadata()
            );
        }

        if (previousStatus == resultingStatus) {
            return PolicyDecision.withProfile(
                    ResultIntentDecision.NONE,
                    "NO_STATE_CHANGE",
                    profile.metadata()
            );
        }

        return PolicyDecision.withProfile(
                profile.decisionFor(resultingStatus),
                failure == null
                        ? "RESULT_INTENT_SELECTED"
                        : failure.failureCode().value(),
                profile.metadata()
        );
    }
}
