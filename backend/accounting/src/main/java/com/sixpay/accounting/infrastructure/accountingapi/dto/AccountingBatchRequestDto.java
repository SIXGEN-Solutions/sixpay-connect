package com.sixpay.accounting.infrastructure.accountingapi.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AccountingBatchRequestDto(
        UUID batchId,
        LocalDate businessDate,
        List<Item> items
) {

    public record Item(
            String paymentReference,
            UUID financialSnapshotId,
            String t0BankReference,
            List<Entry> entries
    ) {
    }

    public record Entry(
            int sequence,
            Account account,
            String currency,
            BigDecimal amount,
            String direction
    ) {
    }

    public record Account(
            String age,
            String ncp,
            String clc
    ) {
    }
}
