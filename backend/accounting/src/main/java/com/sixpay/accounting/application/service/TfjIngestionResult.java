package com.sixpay.accounting.application.service;

import com.sixpay.accounting.domain.model.TfjReceiptStatus;

import java.time.Instant;
import java.util.UUID;

public record TfjIngestionResult(
        UUID confirmationId,
        TfjReceiptStatus receiptStatus,
        Instant receivedAt
) {
}
