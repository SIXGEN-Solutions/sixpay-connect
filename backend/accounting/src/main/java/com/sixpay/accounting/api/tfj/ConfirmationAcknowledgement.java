package com.sixpay.accounting.api.tfj;

import java.time.Instant;
import java.util.UUID;

public record ConfirmationAcknowledgement(
        UUID confirmationId,
        UUID correlationId,
        String receiptStatus,
        Instant receivedAt
) {
}
