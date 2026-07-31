package com.sixpay.payment.domain.model.evidence;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

public enum AuthorizationBindingResult implements ValueObject {
    MATCH,
    MISMATCH,
    NOT_EVALUATED
}
