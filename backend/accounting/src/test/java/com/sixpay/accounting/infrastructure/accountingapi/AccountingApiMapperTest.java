package com.sixpay.accounting.infrastructure.accountingapi;

import com.sixpay.accounting.domain.model.AccountingBatch;
import com.sixpay.accounting.domain.model.AccountingBatchId;
import com.sixpay.accounting.domain.model.AccountingBatchIdempotencyKey;
import com.sixpay.accounting.domain.model.AccountingBatchItem;
import com.sixpay.accounting.domain.model.AccountingBatchItemStatus;
import com.sixpay.accounting.domain.model.AccountingBatchStatus;
import com.sixpay.accounting.infrastructure.accountingapi.mapper.AccountingApiMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AccountingApiMapperTest {

    @Test
    void mapsCanonicalBatchWithoutTfjConcepts() {
        AccountingBatch batch = batch();

        var request =
                new AccountingApiMapper()
                        .toRequest(batch);

        assertEquals(
                batch.batchId().value(),
                request.batchId()
        );
        assertEquals(
                batch.idempotencyKey().value(),
                request.idempotencyKey()
        );
        assertEquals(1, request.items().size());
        assertEquals(
                "XAF",
                request.items()
                        .getFirst()
                        .currency()
        );
    }

    private static AccountingBatch batch() {
        return new AccountingBatch(
                new AccountingBatchId(
                        UUID.fromString(
                                "7ed75090-8af7-4dfa-9b62-8e4dca73501a"
                        )
                ),
                new AccountingBatchIdempotencyKey(
                        "a".repeat(64)
                ),
                LocalDate.of(2026, 8, 7),
                "LAREGIONALE",
                Instant.parse(
                        "2026-08-07T20:00:00Z"
                ),
                AccountingBatchStatus.NOT_COMPLETED,
                List.of(
                        new AccountingBatchItem(
                                UUID.fromString(
                                        "43d7e460-4ca7-4ed1-8603-9f11fb62dd65"
                                ),
                                "PAY-20260807-0001",
                                "TRESORPAY",
                                new BigDecimal("10000"),
                                Currency.getInstance("XAF"),
                                Instant.parse(
                                        "2026-08-07T12:00:00Z"
                                ),
                                LocalDate.of(2026, 8, 7),
                                "AMP-POST-1",
                                "CONFIRMED",
                                Instant.parse(
                                        "2026-08-07T12:05:00Z"
                                ),
                                AccountingBatchItemStatus.PENDING
                        )
                )
        );
    }
}
