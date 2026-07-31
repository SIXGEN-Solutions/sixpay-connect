package com.sixpay.payment.domain.policy;

public enum FailureDispositionDecision {
    BUSINESS_REJECT,
    SECURITY_REJECT,
    TECHNICAL_FAIL_NO_EFFECT,
    DEFER,
    OUTCOME_UNKNOWN,
    REVERSAL_REQUIRED
}
