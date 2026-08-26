package com.sixpay.customer.observation.domain.model;

/**
 * Customer-owned payment status used by the Observed Customer projection.
 */
public enum ObservedPaymentStatus {
    RECEIVED(CountCategory.NEUTRAL),
    AUTHORIZATION_CHECKING(CountCategory.NEUTRAL),
    BANKING_CHECKING(CountCategory.NEUTRAL),
    REJECTED(CountCategory.FAILED),
    APPROVED(CountCategory.NEUTRAL),
    POSTING(CountCategory.NEUTRAL),
    ACCOUNTING_OUTCOME_UNKNOWN(CountCategory.NEUTRAL),
    DEBITED(CountCategory.SUCCESSFUL),
    CUT_CREDITED(CountCategory.SUCCESSFUL),
    REVERSAL_REQUIRED(CountCategory.NEUTRAL),
    REVERSAL_PENDING(CountCategory.NEUTRAL),
    REVERSED(CountCategory.FAILED),
    FAILED(CountCategory.FAILED),
    NOTIFIED(CountCategory.SUCCESSFUL),
    PENDING_END_OF_DAY_CONFIRMATION(CountCategory.NEUTRAL),
    TREASURY_INTEGRATED(CountCategory.SUCCESSFUL);

    private final CountCategory countCategory;

    ObservedPaymentStatus(CountCategory countCategory) {
        this.countCategory = countCategory;
    }

    public boolean countsAsSuccessful() {
        return countCategory == CountCategory.SUCCESSFUL;
    }

    public boolean countsAsFailed() {
        return countCategory == CountCategory.FAILED;
    }

    private enum CountCategory {
        NEUTRAL,
        SUCCESSFUL,
        FAILED
    }
}
