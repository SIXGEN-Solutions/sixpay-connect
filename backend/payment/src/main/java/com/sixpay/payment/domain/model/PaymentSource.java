package com.sixpay.payment.domain.model;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

/**
 * External system owning the original Payment intention.
 */
public enum PaymentSource implements ValueObject {
    TRESOR_PAY
}
