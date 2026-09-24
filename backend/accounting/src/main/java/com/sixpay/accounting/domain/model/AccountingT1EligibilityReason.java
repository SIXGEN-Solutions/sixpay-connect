package com.sixpay.accounting.domain.model;

/**
 * Accounting-owned normalized reason explaining why a T1 candidate is not
 * eligible for the current selection.
 */
public enum AccountingT1EligibilityReason {
    NONE,
    PARTNER_STATUS_NOT_COMPLETED,
    PARTNER_STATUS_UNAVAILABLE,
    OUTSIDE_SELECTION_WINDOW
}
