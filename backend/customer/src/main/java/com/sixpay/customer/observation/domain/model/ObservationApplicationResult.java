package com.sixpay.customer.observation.domain.model;

/**
 * Result of applying an observation to the projection.
 */
public enum ObservationApplicationResult {
    APPLIED_NEW_PAYMENT,
    APPLIED_PAYMENT_UPDATE,
    APPLIED_STALE_HISTORY,
    REPLAYED
}
