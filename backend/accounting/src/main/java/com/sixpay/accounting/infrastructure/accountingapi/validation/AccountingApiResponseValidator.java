package com.sixpay.accounting.infrastructure.accountingapi.validation;

import com.sixpay.accounting.application.exception.AccountingProviderInvalidResponseException;
import com.sixpay.accounting.domain.model.AccountingBatchId;
import com.sixpay.accounting.domain.model.AccountingBatchIdempotencyKey;
import com.sixpay.accounting.infrastructure.accountingapi.dto.AccountingBatchResponseDto;

import java.util.Objects;

public final class AccountingApiResponseValidator {

    public AccountingBatchResponseDto validate(
            AccountingBatchResponseDto response,
            AccountingBatchId expectedBatchId,
            AccountingBatchIdempotencyKey expectedIdempotencyKey
    ) {
        if (response == null
                || response.batchId() == null
                || response.idempotencyKey() == null
                || response.status() == null
                || response.processedAt() == null
                || response.items() == null) {
            throw new AccountingProviderInvalidResponseException(
                    "Accounting API response is incomplete",
                    null
            );
        }

        if (expectedBatchId != null
                && !Objects.equals(
                        expectedBatchId.value(),
                        response.batchId()
                )) {
            throw new AccountingProviderInvalidResponseException(
                    "Accounting API response batchId mismatch",
                    null
            );
        }

        if (expectedIdempotencyKey != null
                && !Objects.equals(
                        expectedIdempotencyKey.value(),
                        response.idempotencyKey()
                )) {
            throw new AccountingProviderInvalidResponseException(
                    "Accounting API response idempotencyKey mismatch",
                    null
            );
        }

        return response;
    }
}
