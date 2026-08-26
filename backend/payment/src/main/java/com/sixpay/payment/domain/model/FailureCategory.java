package com.sixpay.payment.domain.model;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

/**
 * High-level Payment failure classification.
 */
public enum FailureCategory implements ValueObject {
    BUSINESS_REJECTION,
    TECHNICAL_FAILURE,
    UNCERTAIN_EXTERNAL_OUTCOME,
    SECURITY_REJECTION,
    INTEGRATION_CONFLICT,
    TREASURY_RECONCILIATION_FAILURE
}
