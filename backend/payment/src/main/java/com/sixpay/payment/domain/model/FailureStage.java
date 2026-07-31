package com.sixpay.payment.domain.model;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

/**
 * Payment lifecycle stage at which a failure was accepted.
 */
public enum FailureStage implements ValueObject {
    INTAKE,
    AUTHORIZATION,
    BANKING_VERIFICATION,
    FUNDS_CONTROL,
    POSTING,
    POSTING_RESOLUTION,
    NOTIFICATION_INTENT,
    END_OF_DAY_RECONCILIATION,
    REVERSAL
}
