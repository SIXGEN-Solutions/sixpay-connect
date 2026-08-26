package com.sixpay.payment.domain.policy;

public enum PostingAuthorizationDecision {
    AUTHORIZE,
    NO_OP_SAME_INSTRUCTION,
    REJECT_INELIGIBLE,
    REJECT_SECOND_OR_CONFLICTING_INSTRUCTION
}
