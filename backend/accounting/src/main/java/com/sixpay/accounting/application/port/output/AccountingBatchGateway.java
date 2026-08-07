package com.sixpay.accounting.application.port.output;

import com.sixpay.accounting.domain.model.AccountingBatch;
import com.sixpay.accounting.domain.model.AccountingBatchId;
import com.sixpay.accounting.domain.model.AccountingBatchIdempotencyKey;
import com.sixpay.accounting.domain.model.AccountingProviderBatchResult;

import java.util.Optional;

public interface AccountingBatchGateway {

    AccountingProviderBatchResult submit(
            AccountingBatch batch,
            AccountingIntegrationContext context
    );

    Optional<AccountingProviderBatchResult> findByBatchId(
            AccountingBatchId batchId,
            AccountingIntegrationContext context
    );

    Optional<AccountingProviderBatchResult>
    findByIdempotencyKey(
            AccountingBatchIdempotencyKey idempotencyKey,
            AccountingIntegrationContext context
    );
}
