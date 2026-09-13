package com.sixpay.accounting.domain.model;

/**
 * Accounting-owned operational status for a T1 candidate.
 *
 * <p>This model normalizes Accounting facts for operator-facing use without
 * exposing provider or infrastructure implementation details.</p>
 */
public enum AccountingT1OperationalCandidateStatus {
    AWAITING_TRESORPAY_VERIFICATION,
    ELIGIBLE_FOR_BATCH,
    INELIGIBLE_FOR_CURRENT_SELECTION,
    ASSIGNED_TO_BATCH
}
