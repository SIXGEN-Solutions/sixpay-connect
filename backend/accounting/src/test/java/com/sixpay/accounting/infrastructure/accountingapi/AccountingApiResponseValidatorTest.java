package com.sixpay.accounting.infrastructure.accountingapi;

import com.sixpay.accounting.application.exception.AccountingProviderInvalidResponseException;
import com.sixpay.accounting.domain.model.AccountingBatchId;
import com.sixpay.accounting.infrastructure.accountingapi.dto.AccountingBatchResponseDto;
import com.sixpay.accounting.infrastructure.accountingapi.validation.AccountingApiResponseValidator;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AccountingApiResponseValidatorTest {

    private final AccountingApiResponseValidator validator =
            new AccountingApiResponseValidator();

    @Test
    void rejectsFailedItemWithoutRejectionCode() {
        UUID batchId = UUID.randomUUID();

        assertThrows(
                AccountingProviderInvalidResponseException.class,
                () -> validator.validate(
                        new AccountingBatchResponseDto(
                                batchId,
                                "COMPLETED",
                                "AMP-BATCH-1",
                                Instant.parse("2026-09-08T12:00:00Z"),
                                List.of(
                                        new AccountingBatchResponseDto.Item(
                                                "PAY-1",
                                                "FAILED",
                                                null,
                                                null
                                        )
                                )
                        ),
                        new AccountingBatchId(batchId)
                )
        );
    }

    @Test
    void acceptsProcessingResponseWithoutProcessedAt() {
        UUID batchId = UUID.randomUUID();

        validator.validate(
                new AccountingBatchResponseDto(
                        batchId,
                        "PROCESSING",
                        "AMP-BATCH-1",
                        null,
                        List.of(
                                new AccountingBatchResponseDto.Item(
                                        "PAY-1",
                                        "UNKNOWN",
                                        null,
                                        null
                                )
                        )
                ),
                new AccountingBatchId(batchId)
        );
    }
}
