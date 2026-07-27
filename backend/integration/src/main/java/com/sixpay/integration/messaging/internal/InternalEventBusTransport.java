package com.sixpay.integration.messaging.internal;

import com.sixpay.common.messaging.model.IntegrationEventEnvelope;
import com.sixpay.common.messaging.transport.IntegrationEventTransport;
import com.sixpay.common.validation.Preconditions;
import org.springframework.context.ApplicationEventPublisher;

/**
 * In-process transport used while SIXPAY CONNECT runs as a modular monolith.
 */
public final class InternalEventBusTransport
        implements IntegrationEventTransport {

    private final ApplicationEventPublisher eventPublisher;

    public InternalEventBusTransport(
            ApplicationEventPublisher eventPublisher
    ) {
        this.eventPublisher = Preconditions.requireNonNull(
                eventPublisher,
                "Application event publisher must not be null"
        );
    }

    @Override
    public void publish(IntegrationEventEnvelope event) {
        eventPublisher.publishEvent(
                Preconditions.requireNonNull(
                        event,
                        "Integration event must not be null"
                )
        );
    }
}
