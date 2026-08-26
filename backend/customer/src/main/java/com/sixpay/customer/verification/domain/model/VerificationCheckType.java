package com.sixpay.customer.verification.domain.model;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

public enum VerificationCheckType implements ValueObject {
    CUSTOMER_EXISTS,
    FINANCIAL_INSTITUTION_MATCHES,
    NIU_MATCHES,
    IDENTITY_MATCHES,
    ACCOUNT_EXISTS,
    ACCOUNT_BELONGS_TO_CUSTOMER,
    ACCOUNT_IS_ACTIVE,
    ACCOUNT_NOT_BLOCKED,
    ACCOUNT_NOT_OPPOSED,
    REQUIRED_KYC_PRESENT,
    REQUIRED_KYC_VERIFIED
}
