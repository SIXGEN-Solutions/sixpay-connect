package com.sixpay.partner.application.command;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.partner.domain.model.PartnerId;

public record ReactivatePartnerCommand(
        PartnerId partnerId,
        String actorId,
        CorrelationId correlationId,
        String idempotencyKey
) {
}
