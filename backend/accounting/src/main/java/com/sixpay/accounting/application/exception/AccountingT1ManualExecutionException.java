package com.sixpay.accounting.application.exception;

public final class AccountingT1ManualExecutionException
        extends RuntimeException {

    private final Reason reason;

    public AccountingT1ManualExecutionException(
            Reason reason,
            String message
    ) {
        super(message);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }

    public enum Reason {
        NO_CANDIDATE_OR_BATCH,
        MULTIPLE_FINANCIAL_INSTITUTIONS
    }
}
