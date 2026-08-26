package com.sixpay.payment.domain.model;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

/**
 * Approved recovery classification for a Payment failure.
 */
public enum RetryDisposition implements ValueObject {
    NOT_RETRYABLE,
    SAFE_RETRY,
    AUTHORITATIVE_LOOKUP_REQUIRED,
    OPERATOR_ACTION_REQUIRED,
    RECOVERY_EVENT_REQUIRED
}
