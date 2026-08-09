package com.sixpay.partner.application.view;

import com.sixpay.partner.domain.model.PartnerStatus;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record PartnerSummaryView(
        UUID id,
        String legalName,
        String technicalContactName,
        String technicalContactEmail,
        Set<String> authorizedTransactionTypes,
        PartnerStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
