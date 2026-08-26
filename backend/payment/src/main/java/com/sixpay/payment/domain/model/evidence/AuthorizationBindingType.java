package com.sixpay.payment.domain.model.evidence;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

public enum AuthorizationBindingType implements ValueObject {
    SUBSCRIPTION_REFERENCE,
    CLIENT_APPLICATION,
    CUSTOMER_IDENTITY,
    FINANCIAL_INSTITUTION,
    DEBTOR_ACCOUNT,
    EXTERNAL_PAYMENT_REFERENCE,
    PAYMENT_SCOPE,
    TOKEN_REPLAY
}
