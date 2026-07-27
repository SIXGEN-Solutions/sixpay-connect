package com.sixpay.partner.application.view;

import com.sixpay.partner.application.port.out.PartnerAuditRecord;

import java.time.Instant;
import java.util.UUID;

public record PartnerAuditView(
        UUID partnerId,
        String action,
        String result,
        String actorId,
        String correlationId,
        String details,
        Instant occurredAt
) {

    public static PartnerAuditView from(PartnerAuditRecord record) {
        return new PartnerAuditView(
                record.partnerId().value(),
                record.action(),
                record.result(),
                record.actorId(),
                record.correlationId(),
                record.details(),
                record.occurredAt()
        );
    }
}
