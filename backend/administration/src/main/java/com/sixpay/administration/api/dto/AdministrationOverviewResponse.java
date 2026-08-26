package com.sixpay.administration.api.dto;

import com.sixpay.administration.domain.model.AdministrationOverview;

import java.time.Instant;
import java.util.List;

public record AdministrationOverviewResponse(
        AdministrationSettingsResponse settings,
        List<IntegrationStatusResponse> integrations,
        Instant observedAt
) {
    public static AdministrationOverviewResponse from(
            AdministrationOverview overview
    ) {
        return new AdministrationOverviewResponse(
                AdministrationSettingsResponse.from(
                        overview.settings()
                ),
                overview.integrations()
                        .stream()
                        .map(IntegrationStatusResponse::from)
                        .toList(),
                overview.observedAt()
        );
    }
}
