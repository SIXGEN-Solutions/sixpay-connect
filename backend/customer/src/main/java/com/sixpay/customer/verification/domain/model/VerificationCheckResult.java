package com.sixpay.customer.verification.domain.model;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

public enum VerificationCheckResult implements ValueObject {
    PASS,
    FAIL,
    UNKNOWN
}
