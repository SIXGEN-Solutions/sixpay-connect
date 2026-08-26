package com.sixpay.partner.application.command;

import com.sixpay.common.context.CorrelationId;

import java.util.Set;

public record CreatePartnerCommand(
        String legalName,
        String technicalContactName,
        String technicalContactEmail,
        Set<String> authorizedTransactionTypes,
        String actorId,
        CorrelationId correlationId,
        String idempotencyKey
) {
}
