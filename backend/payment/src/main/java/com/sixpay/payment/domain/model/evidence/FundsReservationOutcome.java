package com.sixpay.payment.domain.model.evidence;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

public enum FundsReservationOutcome implements ValueObject {
    RESERVED,
    REJECTED,
    UNKNOWN
}
