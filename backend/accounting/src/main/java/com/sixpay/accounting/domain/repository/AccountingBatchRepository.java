package com.sixpay.accounting.domain.repository;

import com.sixpay.accounting.domain.model.AccountingBatch;
import com.sixpay.accounting.domain.model.AccountingBatchId;
import com.sixpay.accounting.domain.model.AccountingBatchIdempotencyKey;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface AccountingBatchRepository {

    AccountingBatch save(AccountingBatch batch);

    Optional<AccountingBatch> findById(
            AccountingBatchId batchId
    );

    Optional<AccountingBatch> findByIdempotencyKey(
            AccountingBatchIdempotencyKey idempotencyKey
    );

    Set<UUID> findAssignedPaymentIds(
            Set<UUID> paymentIds
    );
}
