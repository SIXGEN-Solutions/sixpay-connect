package com.sixpay.payment.domain.model.authorization;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

public enum SixpayAuthorizationCheckResult implements ValueObject {
    PASS,
    FAIL
}
