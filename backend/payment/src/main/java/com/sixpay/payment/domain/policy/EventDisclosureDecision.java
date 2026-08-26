package com.sixpay.payment.domain.policy;

public enum EventDisclosureDecision {
    ALLOW,
    REJECT_UNDECLARED_FIELD,
    REJECT_CLASSIFICATION,
    REJECT_SENSITIVE_DATA
}
