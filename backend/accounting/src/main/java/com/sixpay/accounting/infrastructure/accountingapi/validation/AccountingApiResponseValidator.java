package com.sixpay.accounting.infrastructure.accountingapi.validation;

import com.sixpay.accounting.application.exception.AccountingProviderInvalidResponseException;
import com.sixpay.accounting.domain.model.AccountingBatchId;
import com.sixpay.accounting.infrastructure.accountingapi.dto.AccountingBatchResponseDto;

import java.util.HashSet;
import java.util.Objects;

public final class AccountingApiResponseValidator {

    public AccountingBatchResponseDto validate(
            AccountingBatchResponseDto response,
            AccountingBatchId expectedBatchId
    ) {
        if (response == null
                || response.batchId() == null
                || response.status() == null
                || response.items() == null) {
            throw invalid("Accounting API response is incomplete");
        }

        if (expectedBatchId != null
                && !Objects.equals(
                        expectedBatchId.value(),
                        response.batchId()
                )) {
            throw invalid("Accounting API response batchId mismatch");
        }

        if ("COMPLETED".equals(response.status())
                && response.processedAt() == null) {
            throw invalid(
                    "Completed Accounting API response requires processedAt"
            );
        }

        var seen = new HashSet<String>();

        for (AccountingBatchResponseDto.Item item : response.items()) {
            if (item == null
                    || item.paymentReference() == null
                    || item.paymentReference().isBlank()
                    || item.status() == null
                    || item.status().isBlank()) {
                throw invalid(
                        "Accounting API item response is incomplete"
                );
            }

            String paymentReference = item.paymentReference().strip();

            if (!seen.add(paymentReference)) {
                throw invalid(
                        "Accounting API response contains duplicate paymentReference"
                );
            }

            if ("FAILED".equals(item.status())
                    && (item.rejectionCode() == null
                    || item.rejectionCode().isBlank())) {
                throw invalid(
                        "Failed Accounting API item requires rejectionCode"
                );
            }
        }

        return response;
    }

    private static AccountingProviderInvalidResponseException invalid(
            String message
    ) {
        return new AccountingProviderInvalidResponseException(message, null);
    }
}
