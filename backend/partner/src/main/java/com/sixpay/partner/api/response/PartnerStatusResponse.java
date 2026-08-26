package com.sixpay.partner.api.response;

import com.sixpay.partner.application.view.PartnerView;
import com.sixpay.partner.domain.model.PartnerStatus;

import java.time.Instant;
import java.util.UUID;

public record PartnerStatusResponse(
        UUID partnerId,
        PartnerStatus status,
        String statusReason,
        PartnerConnectionInfoResponse connection,
        Instant updatedAt
) {

    public static PartnerStatusResponse from(PartnerView view) {
        return new PartnerStatusResponse(
                view.id(),
                view.status(),
                view.statusReason(),
                PartnerConnectionInfoResponse.forStatus(view.status()),
                view.updatedAt()
        );
    }
}
