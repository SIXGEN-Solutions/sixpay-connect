package com.sixpay.payment.domain.model.authorization;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

public enum SixpayAuthorizationDecision implements ValueObject {
    APPROVED,
    REJECTED
}
