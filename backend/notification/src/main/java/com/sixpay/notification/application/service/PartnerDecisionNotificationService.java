package com.sixpay.notification.application.service;

import com.sixpay.common.messaging.model.IntegrationEventEnvelope;
import com.sixpay.common.time.TimeProvider;
import com.sixpay.notification.application.model.NotificationDeliveryRegistration;
import com.sixpay.notification.application.model.PartnerDecisionNotification;
import com.sixpay.notification.application.model.PartnerDecisionNotification.Decision;
import com.sixpay.notification.application.port.in.HandleIntegrationEventUseCase;
import com.sixpay.notification.application.port.out.NotificationDeliveryStore;
import com.sixpay.notification.application.port.out.PartnerNotificationSender;
import com.sixpay.notification.application.port.out.PartnerStatusChangedEventDecoder;

import java.time.Instant;
import java.util.Objects;

public final class PartnerDecisionNotificationService
        implements HandleIntegrationEventUseCase {

    static final String PARTNER_AGGREGATE = "PARTNER";
    static final String STATUS_CHANGED_EVENT =
            "PartnerStatusChangedIntegrationEvent";
    static final int SUPPORTED_SCHEMA_VERSION = 2;

    private final PartnerStatusChangedEventDecoder decoder;
    private final PartnerNotificationSender sender;
    private final NotificationDeliveryStore deliveryStore;
    private final TimeProvider timeProvider;

    public PartnerDecisionNotificationService(
            PartnerStatusChangedEventDecoder decoder,
            PartnerNotificationSender sender,
            NotificationDeliveryStore deliveryStore,
            TimeProvider timeProvider
    ) {
        this.decoder = Objects.requireNonNull(decoder);
        this.sender = Objects.requireNonNull(sender);
        this.deliveryStore = Objects.requireNonNull(deliveryStore);
        this.timeProvider = Objects.requireNonNull(timeProvider);
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

        var notification = new PartnerDecisionNotification(
                event.eventId(),
                event.partnerId(),
                event.recipientEmail(),
                decision,
                event.reason(),
                event.correlationId()
        );
        Instant startedAt = timeProvider.now();
        boolean firstDelivery = deliveryStore.tryStart(
                new NotificationDeliveryRegistration(
                        event.eventId(),
                        event.partnerId(),
                        STATUS_CHANGED_EVENT,
                        event.recipientEmail(),
                        template(decision),
                        event.correlationId(),
                        startedAt
                )
        );
        if (!firstDelivery) {
            return;
        }

        try {
            sender.send(notification);
            deliveryStore.markSent(event.eventId(), timeProvider.now());
        } catch (RuntimeException exception) {
            Instant failedAt = timeProvider.now();
            deliveryStore.markFailed(
                    event.eventId(),
                    errorMessage(exception),
                    failedAt,
                    failedAt
            );
            throw exception;
        }
    }

    private static String template(Decision decision) {
        return switch (decision) {
            case APPROVED -> "partner-activated";
            case REJECTED -> "partner-rejected";
            case SUSPENDED -> "partner-suspended";
        };
    }

    private static String errorMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getName();
        }
        return message.length() <= 2000
                ? message
                : message.substring(0, 2000);
    }
}
