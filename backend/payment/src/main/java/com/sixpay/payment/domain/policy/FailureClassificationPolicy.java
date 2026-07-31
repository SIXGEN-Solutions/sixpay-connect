package com.sixpay.payment.domain.policy;

import com.sixpay.payment.domain.model.FailureCategory;
import com.sixpay.payment.domain.model.PaymentFailure;
import com.sixpay.payment.domain.model.PaymentStatus;

import java.util.Objects;

public final class FailureClassificationPolicy {

    public PolicyDecision<FailureDispositionDecision> decide(
            PaymentFailure failure,
            FinancialEffectKnowledge effectKnowledge,
            PaymentStatus status,
            FailureClassificationProfile profile
    ) {
        Objects.requireNonNull(failure, "Payment failure");
        Objects.requireNonNull(
                effectKnowledge,
                "Financial effect knowledge"
        );
        Objects.requireNonNull(status, "Payment status");
        Objects.requireNonNull(profile, "Failure profile");

        if (!profile.allows(
                failure.failureCategory(),
                failure.retryDisposition()
        )) {
            return result(
                    profile,
                    FailureDispositionDecision.DEFER,
                    "FAILURE_PROFILE_MISMATCH"
            );
        }

        if (effectKnowledge == FinancialEffectKnowledge.UNCERTAIN) {
            return result(
                    profile,
                    FailureDispositionDecision.OUTCOME_UNKNOWN,
                    "FINANCIAL_EFFECT_UNKNOWN"
            );
        }

        if (effectKnowledge
                == FinancialEffectKnowledge.CONFIRMED_PARTIAL
                || effectKnowledge
                == FinancialEffectKnowledge.CONFIRMED_COMPLETE) {
            return result(
                    profile,
                    FailureDispositionDecision.REVERSAL_REQUIRED,
                    "CONFIRMED_FINANCIAL_EFFECT"
            );
        }

        return switch (failure.failureCategory()) {
            case BUSINESS_REJECTION -> result(
                    profile,
                    FailureDispositionDecision.BUSINESS_REJECT,
                    "BUSINESS_REJECTION"
            );
            case SECURITY_REJECTION -> result(
                    profile,
                    FailureDispositionDecision.SECURITY_REJECT,
                    "SECURITY_REJECTION"
            );
            case TECHNICAL_FAILURE -> result(
                    profile,
                    FailureDispositionDecision.TECHNICAL_FAIL_NO_EFFECT,
                    "TECHNICAL_FAILURE_WITHOUT_EFFECT"
            );
            case UNCERTAIN_EXTERNAL_OUTCOME -> result(
                    profile,
                    FailureDispositionDecision.OUTCOME_UNKNOWN,
                    "EXTERNAL_OUTCOME_UNKNOWN"
            );
            case INTEGRATION_CONFLICT,
                    TREASURY_RECONCILIATION_FAILURE -> result(
                    profile,
                    FailureDispositionDecision.DEFER,
                    "MANUAL_OR_RECOVERY_ACTION_REQUIRED"
            );
        };
    }

    private static PolicyDecision<FailureDispositionDecision> result(
            FailureClassificationProfile profile,
            FailureDispositionDecision decision,
            String reason
    ) {
        return PolicyDecision.withProfile(
                decision,
                reason,
                profile.metadata()
        );
    }
}
