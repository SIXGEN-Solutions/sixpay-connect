package com.sixpay.administration.api.dto;

import com.sixpay.administration.domain.model.IntegrationHealth;
import com.sixpay.administration.domain.model.IntegrationStatus;

import java.time.Instant;

public record IntegrationStatusResponse(
        String integrationId,
        String name,
        String type,
        IntegrationHealth health,
        String detail,
        Instant lastSuccessfulAt,
        Instant lastCheckedAt
) {
    public static IntegrationStatusResponse from(
            IntegrationStatus status
    ) {
        return new IntegrationStatusResponse(
                status.integrationId(),
                status.name(),
                status.type(),
                status.health(),
                status.detail(),
                status.lastSuccessfulAt(),
                status.lastCheckedAt()
        );
    }
}
