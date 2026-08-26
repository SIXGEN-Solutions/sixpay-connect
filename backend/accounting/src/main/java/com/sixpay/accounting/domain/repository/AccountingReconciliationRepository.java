package com.sixpay.accounting.domain.repository;

import com.sixpay.accounting.domain.model.AccountingBatch;
import com.sixpay.accounting.domain.model.AccountingBatchTracking;

public interface AccountingReconciliationRepository {

    AccountingBatchTracking saveTracking(
            AccountingBatchTracking tracking
    );

    AccountingBatchTracking saveResult(
            AccountingBatch batch,
            AccountingBatchTracking tracking
    );
}
