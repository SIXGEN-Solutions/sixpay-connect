package com.sixpay.payment.domain.model.evidence;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

public enum BankingVerificationOutcome implements ValueObject {
    VERIFIED,
    REJECTED,
    INDETERMINATE
}
