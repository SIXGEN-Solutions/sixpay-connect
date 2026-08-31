package com.sixpay.payment.infrastructure.messaging.internal;

import com.sixpay.common.messaging.model.IntegrationEventEnvelope;
import com.sixpay.payment.application.port.input.HandlePaymentPostPersistenceEventUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * In-process incoming adapter for relayed Payment outbox events.
 */
@Component
@ConditionalOnProperty(
        prefix = "sixpay.messaging",
        name = "transport",
        havingValue = "internal",
        matchIfMissing = true
)
public final class PaymentInternalIntegrationEventListener {

    private final HandlePaymentPostPersistenceEventUseCase useCase;

    public PaymentInternalIntegrationEventListener(
            HandlePaymentPostPersistenceEventUseCase useCase
    ) {
        this.useCase = Objects.requireNonNull(
                useCase,
                "Payment post-persistence use case is required"
        );
    }

    @EventListener
    public void onIntegrationEvent(IntegrationEventEnvelope event) {
        useCase.handle(
                Objects.requireNonNull(
                        event,
                        "Integration event is required"
                )
        );
    }
}
