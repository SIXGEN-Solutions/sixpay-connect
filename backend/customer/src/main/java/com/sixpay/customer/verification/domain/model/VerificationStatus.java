package com.sixpay.customer.verification.domain.model;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

/**
 * Lifecycle status of a Customer Verification aggregate.
 */
public enum VerificationStatus implements ValueObject {
    REQUESTED,
    COMPLETED
}
