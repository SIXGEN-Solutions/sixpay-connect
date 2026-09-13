package com.sixpay.accounting.domain.model;

/**
 * Bounded operator-safe technical issue categories.
 *
 * <p>Raw provider errors, exceptions, stack traces and transport details must
 * not leak through this model.</p>
 */
public enum AccountingT1TechnicalIssue {
    NONE,
    TRESORPAY_STATUS_LOOKUP_UNAVAILABLE,
    ACCOUNTING_SUBMISSION_OUTCOME_UNKNOWN,
    ACCOUNTING_RECONCILIATION_PENDING
}
