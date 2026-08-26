package com.sixpay.payment.domain.policy;

public enum EndOfDayDecision {
    TREASURY_INTEGRATED,
    REVERSAL_REQUIRED,
    MANUAL_RECONCILIATION,
    NO_OP,
    QUARANTINE_CONFLICT
}
