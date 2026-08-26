package com.sixpay.payment.application.event.projection;

/**
 * Stable logical event types supported by the Observed Customer projection contract.
 */
public enum ObservedCustomerProjectionEventType {
    PAYMENT_CREATED,
    PAYMENT_STATUS_CHANGED,
    PAYMENT_REJECTED,
    PAYMENT_FAILED,
    PAYMENT_DEBIT_CONFIRMED,
    PAYMENT_REVERSED,
    PAYMENT_FINALIZED
}
