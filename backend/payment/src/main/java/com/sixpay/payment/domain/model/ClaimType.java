package com.sixpay.payment.domain.model;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

/**
 * TresorPay claim classification accepted by the Payment domain.
 */
public enum ClaimType implements ValueObject {
    AVI,
    IM7,
    RNF
}
