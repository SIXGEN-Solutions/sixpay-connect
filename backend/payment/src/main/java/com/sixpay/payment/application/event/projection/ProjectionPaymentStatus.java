package com.sixpay.payment.application.event.projection;

/**
 * Stable Payment status vocabulary exposed to the Observed Customer projection.
 */
public enum ProjectionPaymentStatus {
    RECEIVED,
    AUTHORIZATION_CHECKING,
    BANKING_CHECKING,
    REJECTED,
    APPROVED,
    POSTING,
    ACCOUNTING_OUTCOME_UNKNOWN,
    DEBITED,
    CUT_CREDITED,
    REVERSAL_REQUIRED,
    REVERSAL_PENDING,
    REVERSED,
    FAILED,
    NOTIFIED,
    PENDING_END_OF_DAY_CONFIRMATION,
    TREASURY_INTEGRATED
}
