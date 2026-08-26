package com.sixpay.partner.application.view;

import com.sixpay.partner.domain.model.Partner;
import com.sixpay.partner.domain.model.PartnerStatus;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record PartnerView(
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

    public static PartnerView from(Partner partner) {
        return new PartnerView(
                partner.id().value(),
                partner.legalName().value(),
                partner.technicalContact().name(),
                partner.technicalContact().email(),
                partner.authorizedPerimeter().transactionTypes(),
                partner.status(),
                partner.statusReason().orElse(null),
                partner.validationThresholds().stream()
                        .map(ValidationThresholdView::from)
                        .toList(),
                partner.createdAt(),
                partner.updatedAt()
        );
    }
}
