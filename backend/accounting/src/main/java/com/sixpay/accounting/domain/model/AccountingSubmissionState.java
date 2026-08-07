package com.sixpay.accounting.domain.model;

public enum AccountingSubmissionState {
    READY,
    SUBMITTING,
    SUBMITTED,
    OUTCOME_UNKNOWN,
    COMPLETED,
    REJECTED,
    RECONCILIATION_REQUIRED
}
