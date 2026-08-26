package com.sixpay.customer.observation.infrastructure.observability;

/**
 * Bounded failure taxonomy for projection observability.
 */
public enum ObservedCustomerProjectionErrorType {
    OPTIMISTIC_LOCK,
    DATA_INTEGRITY,
    DOMAIN,
    AUDIT,
    TRANSACTION,
    UNEXPECTED
}
