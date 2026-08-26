package com.sixpay.customer.observation.api.observability;

public enum ObservedCustomerQueryResult {
    SUCCESS,
    NOT_FOUND,
    INVALID,
    UNAUTHORIZED,
    FORBIDDEN,
    RATE_LIMITED,
    UNAVAILABLE,
    INTERNAL_ERROR
}
