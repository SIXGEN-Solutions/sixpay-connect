package com.sixpay.payment.infrastructure.callback.relay;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.application.port.output.callback
        .PaymentStatusCallbackDelivery;
import com.sixpay.payment.application.port.output.callback
        .PaymentStatusCallbackMessage;
import com.sixpay.payment.application.port.output.callback
        .PaymentStatusCallbackTransportPort;
import com.sixpay.payment.domain.model.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentCallbackOutboxRelayTest {

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
            );

    @Test
    void deliversAndMarksPublished() {
        PaymentCallbackOutboxCoordinator coordinator =
                Mockito.mock(
                        PaymentCallbackOutboxCoordinator.class
                );
        PaymentCallbackPlanFactory planFactory =
                Mockito.mock(
                        PaymentCallbackPlanFactory.class
                );
        PaymentStatusCallbackTransportPort transport =
                Mockito.mock(
                        PaymentStatusCallbackTransportPort.class
                );

        ClaimedPaymentOutboxEvent event = event();
        PaymentStatusCallbackDelivery delivery = delivery();

        when(coordinator.claim())
                .thenReturn(List.of(event));
        when(planFactory.create(event))
                .thenReturn(
                        PaymentCallbackPlan.deliver(delivery)
                );

        new PaymentCallbackOutboxRelay(
                coordinator,
                planFactory,
                transport
        ).publishAvailableCallbacks();

        verify(transport).send(delivery);
        verify(coordinator).markPublished(EVENT_ID);
        verify(coordinator, never()).markFailed(
                Mockito.any(),
                Mockito.anyInt(),
                Mockito.any()
        );
    }

    @Test
    void skippedEventIsAcknowledgedWithoutTransportCall() {
        PaymentCallbackOutboxCoordinator coordinator =
                Mockito.mock(
                        PaymentCallbackOutboxCoordinator.class
                );
        PaymentCallbackPlanFactory planFactory =
                Mockito.mock(
                        PaymentCallbackPlanFactory.class
                );
        PaymentStatusCallbackTransportPort transport =
                Mockito.mock(
                        PaymentStatusCallbackTransportPort.class
                );

        ClaimedPaymentOutboxEvent event = event();

        when(coordinator.claim())
                .thenReturn(List.of(event));
        when(planFactory.create(event))
                .thenReturn(PaymentCallbackPlan.skip());

        new PaymentCallbackOutboxRelay(
                coordinator,
                planFactory,
                transport
        ).publishAvailableCallbacks();

        verify(transport, never()).send(Mockito.any());
        verify(coordinator).markPublished(EVENT_ID);
    }

    @Test
    void transportFailureSchedulesRetryAndDoesNotPublish() {
        PaymentCallbackOutboxCoordinator coordinator =
                Mockito.mock(
                        PaymentCallbackOutboxCoordinator.class
                );
        PaymentCallbackPlanFactory planFactory =
                Mockito.mock(
                        PaymentCallbackPlanFactory.class
                );
        PaymentStatusCallbackTransportPort transport =
                Mockito.mock(
                        PaymentStatusCallbackTransportPort.class
                );

        ClaimedPaymentOutboxEvent event = event();
        PaymentStatusCallbackDelivery delivery = delivery();
        RuntimeException failure =
                new RuntimeException("partner unavailable");

        when(coordinator.claim())
                .thenReturn(List.of(event));
        when(planFactory.create(event))
                .thenReturn(
                        PaymentCallbackPlan.deliver(delivery)
                );
        Mockito.doThrow(failure)
                .when(transport)
                .send(delivery);

        new PaymentCallbackOutboxRelay(
                coordinator,
                planFactory,
                transport
        ).publishAvailableCallbacks();

        verify(coordinator).markFailed(
                EVENT_ID,
                event.attemptCount(),
                failure
        );
        verify(coordinator, never())
                .markPublished(EVENT_ID);
    }

    private static ClaimedPaymentOutboxEvent event() {
        return new ClaimedPaymentOutboxEvent(
                EVENT_ID,
                UUID.fromString(
                        "11111111-2222-3333-4444-555555555555"
                ),
                "PaymentAuthorizationCheckingStarted",
                "11111111-1111-1111-1111-111111111111",
                Instant.parse(
                        "2026-08-03T10:31:00Z"
                ),
                2
        );
    }

    private static PaymentStatusCallbackDelivery delivery() {
        return new PaymentStatusCallbackDelivery(
                "https://tresorpay.cm/callback",
                CorrelationId.of(
                        "11111111-1111-1111-1111-111111111111"
                ),
                new PaymentStatusCallbackMessage(
                        EVENT_ID,
                        "PAYMENT_STATUS_CHANGED",
                        Instant.parse(
                                "2026-08-03T10:31:00Z"
                        ),
                        "PAY-1234567890ABCDEFGHJKMNPQRS",
                        "AVI-2025-00045678",
                        null,
                        PaymentStatus.PENDING_CONFIRMATION,
                        PaymentStatus.AUTHORIZATION_CHECKING,
                        null,
                        "Customer confirmation accepted",
                        null
                )
        );
    }
}
