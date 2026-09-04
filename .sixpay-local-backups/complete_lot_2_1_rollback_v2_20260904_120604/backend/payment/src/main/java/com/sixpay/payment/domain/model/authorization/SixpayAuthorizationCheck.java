package com.sixpay.payment.domain.model.authorization;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

public enum SixpayAuthorizationCheck implements ValueObject {
    PARTNER_AUTHORIZED,
    SUBSCRIPTION_AUTHORIZED,
    APPLICATION_AUTHORIZED,
    CLAIM_TYPE_AUTHORIZED,
    EXECUTION_DATE_VALID,
    REQUEST_DATA_CONSISTENT
}
