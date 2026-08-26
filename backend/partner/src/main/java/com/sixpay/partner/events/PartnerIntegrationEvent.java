package com.sixpay.partner.events;

import java.time.Instant;
import java.util.UUID;

public sealed interface PartnerIntegrationEvent permits
        PartnerCreatedIntegrationEvent,
        PartnerStatusChangedIntegrationEvent,
        PartnerThresholdConfiguredIntegrationEvent {

    int schemaVersion();

    UUID eventId();

    UUID partnerId();

    String actorId();

    String correlationId();

    Instant occurredAt();
}
