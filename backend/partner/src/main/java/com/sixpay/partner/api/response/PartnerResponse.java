package com.sixpay.partner.api.response;

import com.sixpay.partner.application.view.PartnerView;
import com.sixpay.partner.application.view.ValidationThresholdView;
import com.sixpay.partner.domain.model.PartnerStatus;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record PartnerResponse(
        UUID id,
        String legalName,
        String technicalContactName,
        String technicalContactEmail,
        Set<String> authorizedTransactionTypes,
        PartnerStatus status,
        String statusReason,
        List<ValidationThresholdView> validationThresholds,
        Instant createdAt,
        Instant updatedAt
) {

    public static PartnerResponse from(PartnerView view) {
        return new PartnerResponse(
                view.id(),
                view.legalName(),
                view.technicalContactName(),
                view.technicalContactEmail(),
                view.authorizedTransactionTypes(),
                view.status(),
                view.statusReason(),
                view.validationThresholds(),
                view.createdAt(),
                view.updatedAt()
        );
    }
}
