package com.sixpay.payment.domain.model.evidence;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

public enum FundsControlCheckType implements ValueObject {
    ACCOUNT_EXISTS,
    ACCOUNT_ACTIVE,
    DEBIT_ALLOWED,
    CURRENCY_SUPPORTED,
    AVAILABLE_FUNDS_SUFFICIENT,
    PER_TRANSACTION_LIMIT_NOT_EXCEEDED,
    DAILY_LIMIT_NOT_EXCEEDED,
    OTHER_APPLICABLE_LIMITS_NOT_EXCEEDED
}
