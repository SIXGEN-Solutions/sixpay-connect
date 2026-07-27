package com.sixpay.partner.application.command;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.partner.domain.model.PartnerId;

public record SuspendPartnerCommand(
        PartnerId partnerId,
        String reason,
        String actorId,
        CorrelationId correlationId,
        String idempotencyKey
) {
}
