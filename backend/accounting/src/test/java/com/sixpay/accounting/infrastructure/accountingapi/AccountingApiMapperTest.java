package com.sixpay.accounting.infrastructure.accountingapi;

import com.sixpay.accounting.domain.model.AccountingBatch;
import com.sixpay.accounting.domain.model.AccountingBatchId;
import com.sixpay.accounting.domain.model.AccountingBatchIdempotencyKey;
import com.sixpay.accounting.domain.model.AccountingBatchItem;
import com.sixpay.accounting.domain.model.AccountingBatchItemEntry;
import com.sixpay.accounting.domain.model.AccountingBatchItemStatus;
import com.sixpay.accounting.domain.model.AccountingBatchStatus;
import com.sixpay.accounting.infrastructure.accountingapi.dto.AccountingBatchResponseDto;
import com.sixpay.accounting.infrastructure.accountingapi.mapper.AccountingApiMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccountingApiMapperTest {

    private final AccountingApiMapper mapper =
            new AccountingApiMapper();

    @Test
    void mapsFrozenEntriesToReducedCoreBankingPayload() {
        AccountingBatch batch = batch();

        var request = mapper.toRequest(batch);
        var item = request.items().getFirst();

        assertEquals(batch.batchId().value(), request.batchId());
        assertEquals(batch.businessDate(), request.businessDate());
        assertEquals("PAY-20260908-0001", item.paymentReference());
        assertEquals(
                batch.items().getFirst().financialSnapshotId(),
                item.financialSnapshotId()
        );
        assertEquals("AMP-T0-42", item.t0BankReference());

        var debit = item.entries().get(0);
        var credit = item.entries().get(1);

        assertEquals("00001", debit.account().age());
        assertEquals("12345678901", debit.account().ncp());
        assertEquals("42", debit.account().clc());
        assertEquals("950", debit.currency());
        assertEquals("D", debit.direction());
        assertEquals("C", credit.direction());
    }

    @Test
    void mapsProviderPhysicalStatusesToCanonicalDomainStatuses() {
        AccountingBatch batch = batch();

        var result = mapper.toResult(
                new AccountingBatchResponseDto(
                        batch.batchId().value(),
                        "PROCESSING",
                        "AMP-T1-BATCH-42",
                        null,
                        List.of(
                                new AccountingBatchResponseDto.Item(
                                        "PAY-20260908-0001",
                                        "UNKNOWN",
                                        null,
                                        null
                                )
                        )
                ),
                batch
        );

        assertEquals(
                AccountingBatchStatus.NOT_COMPLETED,
                result.status()
        );
        assertEquals(
                AccountingBatchItemStatus.RECONCILIATION_REQUIRED,
                result.items().getFirst().status()
        );
    }

    @Test
    void rejectsLegacyBatchItemWithoutFrozenSnapshotEvidence() {
        AccountingBatchItem legacy =
                new AccountingBatchItem(
                        UUID.randomUUID(),
                        "PAY-LEGACY",
                        "TRESORPAY",
                        new BigDecimal("1000"),
                        Currency.getInstance("XAF"),
                        Instant.parse("2026-09-08T10:00:00Z"),
                        LocalDate.of(2026, 9, 8),
                        "AMP-T0-LEGACY",
                        "COMPLETED",
                        Instant.parse("2026-09-08T10:01:00Z"),
                        AccountingBatchItemStatus.PENDING
                );

        AccountingBatch batch =
                new AccountingBatch(
                        new AccountingBatchId(UUID.randomUUID()),
                        new AccountingBatchIdempotencyKey("a".repeat(64)),
                        LocalDate.of(2026, 9, 8),
                        "LAREGIONALE",
                        Instant.parse("2026-09-08T11:00:00Z"),
                        AccountingBatchStatus.NOT_COMPLETED,
                        List.of(legacy)
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> mapper.toRequest(batch)
        );
    }

    private static AccountingBatch batch() {
        UUID paymentId =
                UUID.fromString("43d7e460-4ca7-4ed1-8603-9f11fb62dd65");
        UUID snapshotId =
                UUID.fromString("5b72d5b1-2d1e-4f18-a8e0-1c8e19d31d01");

        AccountingBatchItem item =
                new AccountingBatchItem(
                        paymentId,
                        "PAY-20260908-0001",
                        "TRESORPAY",
                        new BigDecimal("10000"),
                        Currency.getInstance("XAF"),
                        Instant.parse("2026-09-08T10:00:00Z"),
                        LocalDate.of(2026, 9, 8),
                        "AMP-T0-42",
                        "COMPLETED",
                        Instant.parse("2026-09-08T10:02:00Z"),
                        AccountingBatchItemStatus.PENDING,
                        snapshotId,
                        "v1",
                        Instant.parse("2026-09-08T10:03:00Z"),
                        "00001-12345678901-42",
                        "00001-98765432109-17",
                        List.of(
                                new AccountingBatchItemEntry(
                                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                                        1,
                                        "DEBIT",
                                        "00001-12345678901-42",
                                        new BigDecimal("10000"),
                                        Currency.getInstance("XAF"),
                                        Instant.parse("2026-09-08T10:03:01Z")
                                ),
                                new AccountingBatchItemEntry(
                                        UUID.fromString("22222222-2222-2222-2222-222222222222"),
                                        2,
                                        "CREDIT",
                                        "00001-98765432109-17",
                                        new BigDecimal("10000"),
                                        Currency.getInstance("XAF"),
                                        Instant.parse("2026-09-08T10:03:02Z")
                                )
                        )
                );

        return new AccountingBatch(
                new AccountingBatchId(
                        UUID.fromString("7ed75090-8af7-4dfa-9b62-8e4dca73501a")
                ),
                new AccountingBatchIdempotencyKey("a".repeat(64)),
                LocalDate.of(2026, 9, 8),
                "LAREGIONALE",
                Instant.parse("2026-09-08T11:00:00Z"),
                AccountingBatchStatus.NOT_COMPLETED,
                List.of(item)
        );
    }
}
