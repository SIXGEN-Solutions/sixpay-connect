package com.sixpay.notification.infrastructure.messaging.internal;

import com.sixpay.common.messaging.model.IntegrationEventEnvelope;
import com.sixpay.common.validation.Preconditions;
import com.sixpay.notification.application.port.in.HandleIntegrationEventUseCase;
import org.springframework.context.event.EventListener;

/**
 * In-process incoming adapter used by the modular monolith.
 */
public final class InternalIntegrationEventListener {

    private final HandleIntegrationEventUseCase useCase;

    public InternalIntegrationEventListener(
            HandleIntegrationEventUseCase useCase
    ) {
        this.useCase = Preconditions.requireNonNull(
                useCase,
                "Integration event use case must not be null"
        );
    }

    @EventListener
    public void onIntegrationEvent(IntegrationEventEnvelope event) {
        useCase.handle(
                Preconditions.requireNonNull(
                        event,
                        "Integration event must not be null"
                )
        );
    }
}
