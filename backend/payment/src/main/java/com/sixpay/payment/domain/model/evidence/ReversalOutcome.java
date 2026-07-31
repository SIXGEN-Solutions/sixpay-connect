package com.sixpay.payment.domain.model.evidence;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

public enum ReversalOutcome implements ValueObject {
    REVERSED,
    REJECTED,
    NOT_ALLOWED,
    UNKNOWN
}
