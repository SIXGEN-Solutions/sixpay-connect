package com.sixpay.accounting.api.response;

import com.sixpay.accounting.application.port.input.AccountingT1ManualExecutionUseCase;

import java.time.LocalDate;
import java.util.UUID;

public record AccountingT1ManualExecutionResponse(
        UUID batchId,
        LocalDate businessDate,
        String financialInstitutionCode,
        String batchStatus,
        String submissionState,
        String providerBatchReference
) {
    public static AccountingT1ManualExecutionResponse from(
            AccountingT1ManualExecutionUseCase.Result result
    ) {
        return new AccountingT1ManualExecutionResponse(
                result.batch().batchId().value(),
                result.batch().businessDate(),
                result.batch().financialInstitutionCode(),
                result.batch().status().name(),
                result.tracking().submissionState().name(),
                result.tracking().providerBatchReference()
        );
    }
}
