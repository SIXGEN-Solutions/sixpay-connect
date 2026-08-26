package com.sixpay.payment.domain.model.evidence;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

public enum EvidenceCheckResult implements ValueObject {
    PASS,
    FAIL,
    UNKNOWN
}
