package com.sixpay.payment.domain.model.evidence;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

public enum ReversalAuthorizationType implements ValueObject {
    BANK_INSTRUCTION,
    APPROVED_RUNBOOK
}
