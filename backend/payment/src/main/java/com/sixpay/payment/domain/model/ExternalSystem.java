package com.sixpay.payment.domain.model;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

/**
 * Closed system classification used by PaymentFailure.
 */
public enum ExternalSystem implements ValueObject {
    TRESOR_PAY,
    AMPLITUDE,
    SIXPAY,
    NOT_APPLICABLE
}
