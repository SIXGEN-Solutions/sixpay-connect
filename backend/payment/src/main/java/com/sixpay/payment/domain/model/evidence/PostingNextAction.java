package com.sixpay.payment.domain.model.evidence;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

public enum PostingNextAction implements ValueObject {
    NONE,
    QUERY_OUTCOME,
    WAIT_FOR_CUT_CREDIT,
    OPEN_RECONCILIATION,
    REQUEST_EXPLICIT_REVERSAL
}
