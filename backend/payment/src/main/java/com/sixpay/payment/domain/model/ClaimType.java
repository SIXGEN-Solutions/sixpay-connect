package com.sixpay.payment.domain.model;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

/**
 * Treasury-payment claim classification.
 *
 * <p>This is not a universal Payment invariant. The currently supported
 * values originate from the approved Treasury-payment capability and must not
 * be generalized to unrelated partner payment flows.</p>
 */
public enum ClaimType implements ValueObject {
    AVI,
    IM7,
    RNF
}
