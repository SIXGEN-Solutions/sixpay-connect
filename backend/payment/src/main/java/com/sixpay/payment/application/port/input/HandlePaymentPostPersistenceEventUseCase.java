package com.sixpay.payment.application.port.input;

import com.sixpay.common.messaging.model.IntegrationEventEnvelope;

/**
 * Transport-neutral entry point for durable Payment workflow events.
 */
@FunctionalInterface
public interface HandlePaymentPostPersistenceEventUseCase {

    void handle(IntegrationEventEnvelope event);
}
