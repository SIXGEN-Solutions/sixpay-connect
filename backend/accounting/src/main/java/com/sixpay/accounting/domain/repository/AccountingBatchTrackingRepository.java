package com.sixpay.accounting.domain.repository;

import com.sixpay.accounting.domain.model.AccountingBatchId;
import com.sixpay.accounting.domain.model.AccountingBatchTracking;

import java.util.Optional;

public interface AccountingBatchTrackingRepository {

    AccountingBatchTracking save(
            AccountingBatchTracking tracking
    );

    Optional<AccountingBatchTracking> findByBatchId(
            AccountingBatchId batchId
    );
}