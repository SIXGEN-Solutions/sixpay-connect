package com.sixpay.payment.infrastructure.callback.relay;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.application.port.output.callback
        .PaymentStatusCallbackDelivery;
import com.sixpay.payment.application.port.output.callback
        .PaymentStatusCallbackMessage;
import com.sixpay.payment.application.port.output.callback
        .PaymentStatusCallbackTransportPort;
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
        verify(coordinator).markPublished(EVENT_ID, event.attemptCount());
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
        verify(coordinator).markPublished(EVENT_ID, event.attemptCount());
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
                .markPublished(EVENT_ID, event.attemptCount());
    }

    @Test
    void retryKeepsOneLogicalEventWhileUsingNewDeliveryId() {
        PaymentCallbackOutboxCoordinator coordinator = Mockito.mock(PaymentCallbackOutboxCoordinator.class);
        PaymentCallbackPlanFactory planFactory = Mockito.mock(PaymentCallbackPlanFactory.class);
        PaymentStatusCallbackTransportPort transport = Mockito.mock(PaymentStatusCallbackTransportPort.class);
        ClaimedPaymentOutboxEvent first = event();
        ClaimedPaymentOutboxEvent second = new ClaimedPaymentOutboxEvent(
                first.eventId(), first.paymentId(), first.eventType(),
                first.correlationId(), first.occurredAt(), first.attemptCount() + 1,
                first.payload());
        PaymentStatusCallbackDelivery one = delivery();
        PaymentStatusCallbackDelivery two = new PaymentStatusCallbackDelivery(
                one.callbackUrl(), one.correlationId(), UUID.randomUUID(),
                second.attemptCount(), one.message());
        when(coordinator.claim()).thenReturn(List.of(first), List.of(second));
        when(planFactory.create(first)).thenReturn(PaymentCallbackPlan.deliver(one));
        when(planFactory.create(second)).thenReturn(PaymentCallbackPlan.deliver(two));
        RuntimeException timeout = new RuntimeException("temporary HTTP timeout");
        Mockito.doThrow(timeout).doNothing().when(transport)
                .send(Mockito.any(PaymentStatusCallbackDelivery.class));

        PaymentCallbackOutboxRelay relay = new PaymentCallbackOutboxRelay(
                coordinator, planFactory, transport);
        relay.publishAvailableCallbacks();
        relay.publishAvailableCallbacks();

        org.junit.jupiter.api.Assertions.assertEquals(
                one.message(), two.message(), "Immutable logical payload must be identical");
        org.junit.jupiter.api.Assertions.assertEquals(
                one.message().eventId(), two.message().eventId());
        org.junit.jupiter.api.Assertions.assertNotEquals(
                one.deliveryId(), two.deliveryId());
        verify(transport).send(one);
        verify(transport).send(two);
        verify(coordinator).markFailed(EVENT_ID, first.attemptCount(), timeout);
        verify(coordinator).markPublished(EVENT_ID, second.attemptCount());
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
                2,
                "{}"
        );
    }

    private static PaymentStatusCallbackDelivery delivery() {
        return new PaymentStatusCallbackDelivery(
                "https://tresorpay.cm/callback",
                CorrelationId.of(
                        "11111111-1111-1111-1111-111111111111"
                ),
                UUID.fromString(
                        "99999999-8888-7777-6666-555555555555"
                ),
                2,
                new PaymentStatusCallbackMessage(
                        "1.0",
                        EVENT_ID,
                        PaymentStatusCallbackMessage.CUT_CREDITED,
                        Instant.parse("2026-08-03T10:31:00Z"),
                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        null,
                        UUID.fromString("11111111-2222-3333-4444-555555555555"),
                        "PAY-1234567890ABCDEFGHJKMNPQRS",
                        "AVI-2025-00045678",
                        "LRB",
                        2L,
                        new PaymentStatusCallbackMessage.CutCreditedData(
                                new PaymentStatusCallbackMessage.MoneyData("1000", "XAF"),
                                "BANK-EVENT-123",
                                Instant.parse("2026-08-03T10:31:00Z"),
                                "POSTED_PENDING_TFJ",
                                false
                        )
                )
        );
    }
}
