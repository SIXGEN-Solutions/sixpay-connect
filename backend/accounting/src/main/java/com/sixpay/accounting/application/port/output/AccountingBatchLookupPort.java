package com.sixpay.accounting.application.port.output;

import com.sixpay.accounting.domain.model.AccountingBatch;
import com.sixpay.accounting.domain.model.AccountingBatchId;
import com.sixpay.accounting.domain.model.AccountingBatchIdempotencyKey;

import java.util.Optional;

/**
 * Local Accounting persistence lookup boundary introduced before the external
 * Accounting API client. External provider lookups use AccountingBatchGateway.
 */
public interface AccountingBatchLookupPort {

    Optional<AccountingBatch> findById(
            AccountingBatchId batchId
    );

    Optional<AccountingBatch> findByIdempotencyKey(
            AccountingBatchIdempotencyKey idempotencyKey
    );
}
