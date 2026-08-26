package com.sixpay.payment.domain.policy;

public enum EvidenceTemporalDecision {
    VALID,
    STALE,
    FUTURE_DATED,
    INVALID_CHRONOLOGY
}
