package com.sixpay.administration.domain.model;

import java.time.Instant;
import java.util.Objects;

public record IntegrationStatus(
        String integrationId,
        String name,
        String type,
        IntegrationHealth health,
        String detail,
        Instant lastSuccessfulAt,
        Instant lastCheckedAt
) {
    public IntegrationStatus {
        Objects.requireNonNull(
                integrationId,
                "integrationId"
        );
        Objects.requireNonNull(
                name,
                "name"
        );
        Objects.requireNonNull(
                type,
                "type"
        );
        Objects.requireNonNull(
                health,
                "health"
        );
        Objects.requireNonNull(
                lastCheckedAt,
                "lastCheckedAt"
        );
    }
}
