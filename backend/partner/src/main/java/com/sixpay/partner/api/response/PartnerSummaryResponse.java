package com.sixpay.partner.api.response;

import com.sixpay.partner.application.view.PartnerSummaryView;
import com.sixpay.partner.domain.model.PartnerStatus;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record PartnerSummaryResponse(
        UUID id,
        String legalName,
        String technicalContactName,
        String technicalContactEmail,
        Set<String> authorizedTransactionTypes,
        PartnerStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public static PartnerSummaryResponse from(PartnerSummaryView view) {
        return new PartnerSummaryResponse(
                view.id(),
                view.legalName(),
                view.technicalContactName(),
                view.technicalContactEmail(),
                view.authorizedTransactionTypes(),
                view.status(),
                view.createdAt(),
                view.updatedAt()
        );
    }
}
