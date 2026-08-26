package com.sixpay.accounting.application.exception;

import com.sixpay.accounting.domain.model.AccountingBatchId;

public final class AccountingBatchNotFoundException
        extends RuntimeException {

    public AccountingBatchNotFoundException(
            AccountingBatchId batchId
    ) {
        super(
                "Accounting batch not found: "
                        + batchId
        );
    }
}
