package com.sixpay.payment.domain.model;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

/**
 * Closed IA-1 Payment lifecycle status.
 */
public enum PaymentStatus implements ValueObject {
    RECEIVED(false),
    PENDING_CONFIRMATION(false),
    AUTHORIZATION_CHECKING(false),
    BANKING_VERIFICATION_PENDING(false),
    FUNDS_CONTROL_PENDING(false),
    TREASURY_ACCOUNT_RESOLUTION_PENDING(false),
    APPROVED_FOR_POSTING(false),
    POSTING_PENDING(false),
    POSTING_OUTCOME_UNKNOWN(false),
    DEBIT_CONFIRMED(false),
    POSTED_PENDING_TFJ(false),
    REVERSAL_REQUIRED(false),
    REVERSAL_PENDING(false),
    REVERSAL_OUTCOME_UNKNOWN(false),
    REJECTED(true),
    FAILED(true),
    TREASURY_INTEGRATED(true),
    REVERSED(true);

    private final boolean terminal;

    PaymentStatus(boolean terminal) {
        this.terminal = terminal;
    }

    public boolean isTerminal() {
        return terminal;
    }
}
