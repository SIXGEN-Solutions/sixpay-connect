package com.sixpay.payment.domain.model.evidence;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

public enum FundsReleaseOutcome implements ValueObject {
    RELEASED,
    ALREADY_RELEASED,
    REJECTED,
    UNKNOWN
}
