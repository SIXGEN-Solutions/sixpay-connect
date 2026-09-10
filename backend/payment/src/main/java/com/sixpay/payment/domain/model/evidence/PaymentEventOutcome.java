package com.sixpay.payment.domain.model.evidence;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

public enum PaymentEventOutcome implements ValueObject {
    COMPLETED,
    REJECTED,
    UNKNOWN
}
