package com.sixpay.payment.domain.model.evidence;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

public enum FundsControlOutcome implements ValueObject {
    VERIFIED,
    REJECTED,
    INDETERMINATE
}
