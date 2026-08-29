package com.sixpay.notification.application.port.input;

import com.sixpay.common.messaging.model.IntegrationEventEnvelope;

/**
 * Transport-neutral entry point for events handled by Notification.
 *
 * <p>The business implementation decides whether an event produces an
 * email, SMS, push notification or no notification.</p>
 */
@FunctionalInterface
public interface HandleIntegrationEventUseCase {

    void handle(IntegrationEventEnvelope event);
}
