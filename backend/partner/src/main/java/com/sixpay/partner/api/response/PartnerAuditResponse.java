package com.sixpay.partner.api.response;

import com.sixpay.partner.application.view.PartnerAuditView;

import java.time.Instant;
import java.util.UUID;

/**
 * Public API representation of an immutable Partner audit entry.
 */
public record PartnerAuditResponse(
        UUID partnerId,
        String action,
        String result,
        String actorId,
        String correlationId,
        String details,
        Instant occurredAt
) {

    public static PartnerAuditResponse from(PartnerAuditView view) {
        return new PartnerAuditResponse(
                view.partnerId(),
                view.action(),
                view.result(),
                view.actorId(),
                view.correlationId(),
                view.details(),
                view.occurredAt()
        );
    }
}