package com.sixpay.customer.observation.api.observability;

public enum ObservedCustomerQueryErrorType {
    NONE,
    INVALID_CURSOR,
    INVALID_FILTER,
    INVALID_IDENTIFIER,
    NOT_FOUND,
    AUTHENTICATION,
    AUTHORIZATION,
    RATE_LIMIT,
    TEMPORARY_UNAVAILABLE,
    INTERNAL
}
