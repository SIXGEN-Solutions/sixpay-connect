package com.sixpay.payment.domain.policy;

/**
 * SIXPAY-owned controls evaluated during AUTHORIZATION_CHECKING.
 *
 * <p>This enum defines the bounded control catalogue only. Evaluation rules
 * belong to LOT 2.1.2 and must use the source mapping defined by
 * {@link AuthorizationControlSourceMap}.</p>
 */
public enum AuthorizationControl {
    PARTNER_AUTHORIZED,
    SUBSCRIPTION_AUTHORIZED,
    APPLICATION_AUTHORIZED,
    CLAIM_TYPE_AUTHORIZED,
    EXECUTION_DATE_VALID,
    REQUEST_DATA_CONSISTENT
}
