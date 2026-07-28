package com.sixpay.notification.application.service;

import com.sixpay.common.messaging.model.IntegrationEventEnvelope;
import com.sixpay.notification.application.model.PartnerDecisionNotification;
import com.sixpay.notification.application.model.PartnerDecisionNotification.Decision;
import com.sixpay.notification.application.port.in.HandleIntegrationEventUseCase;
import com.sixpay.notification.application.port.out.PartnerNotificationSender;
import com.sixpay.notification.application.port.out.PartnerStatusChangedEventDecoder;

import java.util.Objects;

public final class PartnerDecisionNotificationService
        implements HandleIntegrationEventUseCase {

    static final String PARTNER_AGGREGATE = "PARTNER";
    static final String STATUS_CHANGED_EVENT =
            "PartnerStatusChangedIntegrationEvent";
    static final int SUPPORTED_SCHEMA_VERSION = 2;

    private final PartnerStatusChangedEventDecoder decoder;
    private final PartnerNotificationSender sender;

    public PartnerDecisionNotificationService(
            PartnerStatusChangedEventDecoder decoder,
            PartnerNotificationSender sender
    ) {
        this.decoder = Objects.requireNonNull(decoder);
        this.sender = Objects.requireNonNull(sender);
    }

    @Override
    public void handle(IntegrationEventEnvelope envelope) {
        Objects.requireNonNull(envelope, "event is required");
        if (!PARTNER_AGGREGATE.equals(envelope.aggregateType())
                || !STATUS_CHANGED_EVENT.equals(envelope.eventType())) {
            return;
        }
        if (envelope.schemaVersion() != SUPPORTED_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "unsupported PartnerStatusChangedIntegrationEvent schema: "
                            + envelope.schemaVersion()
            );
        }

        var event = decoder.decode(envelope.payload());
        if (!envelope.eventId().equals(event.eventId())
                || !envelope.aggregateId().equals(event.partnerId())
                || !envelope.correlationId().equals(event.correlationId())) {
            throw new IllegalArgumentException(
                    "partner status event payload does not match its envelope"
            );
        }

        var decision = switch (event.currentStatus()) {
            case "ACTIVE" -> Decision.APPROVED;
            case "REJECTED" -> Decision.REJECTED;
            case "SUSPENDED" -> Decision.SUSPENDED;
            default -> null;
        };
        if (decision == null) {
            return;
        }

        sender.send(new PartnerDecisionNotification(
                event.eventId(),
                event.partnerId(),
                event.recipientEmail(),
                decision,
                event.reason(),
                event.correlationId()
        ));
    }
}
