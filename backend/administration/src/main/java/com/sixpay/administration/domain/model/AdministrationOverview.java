package com.sixpay.administration.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record AdministrationOverview(
        AdministrationSettings settings,
        List<IntegrationStatus> integrations,
        Instant observedAt
) {
    public AdministrationOverview {
        Objects.requireNonNull(
                settings,
                "settings"
        );
        integrations = List.copyOf(
                Objects.requireNonNull(
                        integrations,
                        "integrations"
                )
        );
        Objects.requireNonNull(
                observedAt,
                "observedAt"
        );
    }
}
