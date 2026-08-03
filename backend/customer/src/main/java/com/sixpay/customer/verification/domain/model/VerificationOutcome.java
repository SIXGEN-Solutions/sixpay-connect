package com.sixpay.customer.verification.domain.model;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

public enum VerificationOutcome implements ValueObject {
    VERIFIED,
    REJECTED,
    INDETERMINATE
}
