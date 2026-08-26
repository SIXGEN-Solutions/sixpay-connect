package com.sixpay.customer.observation.application.audit;

public enum ObservedCustomerAuditOutcome {
    SUCCEEDED,
    REPLAYED,
    IGNORED,
    DENIED,
    FAILED
}
