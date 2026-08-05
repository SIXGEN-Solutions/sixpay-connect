package com.sixpay.customer.observation.infrastructure.observability;

/**
 * Bounded projection result values used in metrics and operational logs.
 */
public enum ObservedCustomerProjectionResultType {
    APPLIED,
    REPLAYED,
    IGNORED_STALE,
    FAILED
}
