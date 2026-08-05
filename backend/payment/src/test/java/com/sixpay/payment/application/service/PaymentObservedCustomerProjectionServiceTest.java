package com.sixpay.payment.application.service;

import com.sixpay.payment.application.event.projection.ObservedCustomerProjectionEvent;
import com.sixpay.payment.application.event.projection.ObservedCustomerProjectionEventType;
import com.sixpay.payment.application.event.projection.ObservedCustomerProjectionPayload;
import com.sixpay.payment.application.event.projection.ProjectionPaymentStatus;
import com.sixpay.payment.application.port.output.ObservedCustomerProjectionPort;
import com.sixpay.payment.application.port.output.ObservedCustomerProjectionResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentObservedCustomerProjectionServiceTest {

    @Test
    void projectsOnlyTheDurableEventSnapshot() {
        ObservedCustomerProjectionEvent event = event();
        ObservedCustomerProjectionPort port =
                mock(ObservedCustomerProjectionPort.class);

        when(port.project(any())).thenReturn(
                new ObservedCustomerProjectionResult(
                        event.eventId(),
                        ObservedCustomerProjectionResult.Disposition.APPLIED,
                        3
                )
        );

        PaymentObservedCustomerProjectionService service =
                new PaymentObservedCustomerProjectionService(
                        port,
                        new PaymentObservedCustomerProjectionRequestFactory()
                );

        ObservedCustomerProjectionResult result = service.project(event);

        assertEquals(event.eventId(), result.sourceEventId());
        assertEquals(
                ObservedCustomerProjectionResult.Disposition.APPLIED,
                result.disposition()
        );
        verify(port).project(any());
    }

    private static ObservedCustomerProjectionEvent event() {
        Instant createdAt = Instant.parse("2026-08-04T00:55:00Z");
        Instant occurredAt = Instant.parse("2026-08-04T01:00:00Z");

        return ObservedCustomerProjectionEvent.versionOne(
                UUID.fromString("11111111-1111-4111-8111-111111111111"),
                UUID.fromString("7ed75090-8af7-4dfa-9b62-8e4dca73501a"),
                8,
                ObservedCustomerProjectionEventType.PAYMENT_REJECTED,
                new ObservedCustomerProjectionPayload(
                        "PAY-2026-000123",
                        "M0123456",
                        "Société ABC SARL",
                        "***-***-1234",
                        "a***@example.com",
                        "AMPLITUDE",
                        "v1:" + "a".repeat(64),
                        "•••• 1234",
                        new BigDecimal("15000.00"),
                        "XAF",
                        ProjectionPaymentStatus.REJECTED,
                        "ACCOUNT_NOT_FOUND",
                        createdAt,
                        occurredAt
                ),
                "c74e165f-df46-463e-a520-188e6df3e5ae",
                occurredAt
        );
    }
}
