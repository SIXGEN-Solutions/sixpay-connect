package com.sixpay.payment.domain.model.evidence;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

public enum PostingLegStatus implements ValueObject {
    NOT_STARTED,
    PENDING,
    SUCCEEDED,
    FAILED,
    UNKNOWN
}
