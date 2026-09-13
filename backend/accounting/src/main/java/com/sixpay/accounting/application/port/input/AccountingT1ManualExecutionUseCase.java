package com.sixpay.accounting.application.port.input;

import com.sixpay.accounting.domain.model.AccountingBatch;
import com.sixpay.accounting.domain.model.AccountingBatchTracking;
import com.sixpay.common.context.CorrelationId;

import java.time.LocalDate;

public interface AccountingT1ManualExecutionUseCase {

    Result execute(LocalDate businessDate, CorrelationId correlationId);

    record Result(
            AccountingBatch batch,
            AccountingBatchTracking tracking
    ) {
    }
}
