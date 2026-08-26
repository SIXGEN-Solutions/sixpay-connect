package com.sixpay.payment.domain.policy;

public enum ReversalDecision {
    AUTHORIZE,
    REVERSED,
    REVERSAL_OUTCOME_UNKNOWN,
    REVERSAL_REQUIRED,
    NO_OP,
    CONFLICT
}
