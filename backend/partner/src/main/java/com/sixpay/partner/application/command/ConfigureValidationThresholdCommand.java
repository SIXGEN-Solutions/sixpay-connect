package com.sixpay.partner.application.command;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.partner.domain.model.PartnerId;

import java.math.BigDecimal;

public record ConfigureValidationThresholdCommand(
        PartnerId partnerId,
        String transactionType,
        String currency,
        BigDecimal amount,
        int validationLevels,
        String actorId,
        CorrelationId correlationId,
        String idempotencyKey
) {
}
