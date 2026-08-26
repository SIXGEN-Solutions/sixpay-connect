package com.sixpay.payment.domain.policy;

public enum EvidenceReplayDecision {
    ACCEPT_NEW,
    NO_OP_IDENTICAL,
    REPLACE_MORE_AUTHORITATIVE,
    REJECT_CONFLICT,
    QUARANTINE_CONFLICT,
    REJECT_TERMINAL_REPLACEMENT
}
