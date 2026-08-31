package com.sixpay.payment.infrastructure.messaging.internal;

import com.sixpay.common.messaging.model.IntegrationEventEnvelope;
import com.sixpay.payment.application.port.input.HandlePaymentPostPersistenceEventUseCase;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PaymentInternalIntegrationEventListenerTest {

    @Test
    void delegatesTransportNeutralEnvelopeToApplicationUseCase() {
        HandlePaymentPostPersistenceEventUseCase useCase =
                mock(HandlePaymentPostPersistenceEventUseCase.class);

        var listener = new PaymentInternalIntegrationEventListener(useCase);

        var event = new IntegrationEventEnvelope(
                UUID.randomUUID(),
                "PaymentReceived",
                1,
                "PAYMENT",
                UUID.randomUUID(),
                UUID.randomUUID().toString(),
                Instant.parse("2026-08-31T16:20:00Z"),
                "{\"event\":\"test\"}"
        );

        listener.onIntegrationEvent(event);

        verify(useCase).handle(event);
    }
}
