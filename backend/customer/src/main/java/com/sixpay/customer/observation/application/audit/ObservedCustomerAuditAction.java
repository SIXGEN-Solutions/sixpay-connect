package com.sixpay.customer.observation.application.audit;

public enum ObservedCustomerAuditAction {
    PROJECTION_APPLIED,
    PROJECTION_REPLAYED,
    PROJECTION_STALE_IGNORED,
    PROJECTION_REJECTED,
    PROJECTION_FAILED,
    QUERY_SEARCHED,
    QUERY_DETAIL_READ,
    QUERY_PAYMENTS_LISTED,
    QUERY_DENIED,
    QUERY_FAILED
}
