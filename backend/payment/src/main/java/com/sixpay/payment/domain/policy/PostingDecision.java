package com.sixpay.payment.domain.policy;

public enum PostingDecision {
    POSTED_PENDING_TFJ,
    DEBIT_CONFIRMED,
    POSTING_OUTCOME_UNKNOWN,
    REJECTED_NO_EFFECT,
    FAILED_NO_EFFECT,
    REVERSAL_REQUIRED,
    NO_OP,
    CONFLICT
}
