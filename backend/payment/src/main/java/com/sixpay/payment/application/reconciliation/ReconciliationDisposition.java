package com.sixpay.payment.application.reconciliation;

public enum ReconciliationDisposition {
    RESOLVED,
    WAIT_AND_QUERY_AGAIN,
    REQUEST_EXPLICIT_REVERSAL,
    OPEN_MANUAL_RECONCILIATION
}
