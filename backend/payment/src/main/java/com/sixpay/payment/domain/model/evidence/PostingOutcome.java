package com.sixpay.payment.domain.model.evidence;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

public enum PostingOutcome implements ValueObject {
    COMPLETED,
    REJECTED_NO_FINANCIAL_EFFECT,
    DEBIT_CONFIRMED_CUT_CREDIT_PENDING,
    REVERSAL_REQUIRED,
    UNKNOWN
}
