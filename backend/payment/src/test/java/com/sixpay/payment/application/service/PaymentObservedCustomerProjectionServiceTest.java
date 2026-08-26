package com.sixpay.payment.application.service;

import com.sixpay.payment.application.event.projection
        .ObservedCustomerProjectionEvent;
import com.sixpay.payment.application.event.projection
        .ObservedCustomerProjectionEventType;
import com.sixpay.payment.application.event.projection
        .ObservedCustomerProjectionPayload;
import com.sixpay.payment.application.event.projection
        .ProjectionPaymentStatus;
import com.sixpay.payment.application.port.output
        .ObservedCustomerProjectionPort;
import com.sixpay.payment.application.port.output
        .ObservedCustomerProjectionRequest;
import com.sixpay.payment.application.port.output
        .ObservedCustomerProjectionResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentObservedCustomerProjectionServiceTest {

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "11111111-1111-4111-8111-111111111111"
            );

    private static final UUID PAYMENT_ID =
            UUID.fromString(
                    "7ed75090-8af7-4dfa-9b62-8e4dca73501a"
            );

    private static final Instant CREATED_AT =
            Instant.parse("2026-08-04T00:55:00Z");

    private static final Instant OCCURRED_AT =
            Instant.parse("2026-08-04T01:00:00Z");

    @Test
    void projectsOnlyTheDurableEventSnapshot() {
        ObservedCustomerProjectionEvent event =
                event(ProjectionPaymentStatus.RECEIVED);

        ObservedCustomerProjectionPort port =
                mock(ObservedCustomerProjectionPort.class);

        ArgumentCaptor<ObservedCustomerProjectionRequest> request =
                ArgumentCaptor.forClass(
                        ObservedCustomerProjectionRequest.class
                );

        when(port.project(request.capture()))
                .thenReturn(
                        new ObservedCustomerProjectionResult(
                                event.eventId(),
                                ObservedCustomerProjectionResult
                                        .Disposition.APPLIED,
                                3
                        )
                );

        PaymentObservedCustomerProjectionService service =
                new PaymentObservedCustomerProjectionService(
                        port,
                        new PaymentObservedCustomerProjectionRequestFactory()
                );

        ObservedCustomerProjectionResult result =
                service.project(event);

        verify(port).project(request.getValue());

        assertEquals(
                event.eventId(),
                result.sourceEventId()
        );

        assertEquals(
                ObservedCustomerProjectionResult.Disposition.APPLIED,
                result.disposition()
        );

        assertEquals(
                ObservedCustomerProjectionRequest
                        .ProjectionPaymentStatus.RECEIVED,
                request.getValue().paymentStatus()
        );

        assertEquals(
                EVENT_ID,
                request.getValue().sourceEventId()
        );

        assertEquals(
                PAYMENT_ID,
                request.getValue().paymentId()
        );

        assertEquals(
                OCCURRED_AT,
                request.getValue().observedAt()
        );

        assertEquals(
                "c74e165f-df46-463e-a520-188e6df3e5ae",
                request.getValue().correlationId()
        );
    }

    @Test
    void historicalReceivedSnapshotIsNotReinterpretedAsRejected() {
        ObservedCustomerProjectionEvent historicalReceived =
                event(ProjectionPaymentStatus.RECEIVED);

        ObservedCustomerProjectionPort port =
                mock(ObservedCustomerProjectionPort.class);

        ArgumentCaptor<ObservedCustomerProjectionRequest> request =
                ArgumentCaptor.forClass(
                        ObservedCustomerProjectionRequest.class
                );

        when(port.project(request.capture()))
                .thenReturn(
                        new ObservedCustomerProjectionResult(
                                EVENT_ID,
                                ObservedCustomerProjectionResult
                                        .Disposition.APPLIED,
                                1
                        )
                );

        PaymentObservedCustomerProjectionService service =
                new PaymentObservedCustomerProjectionService(
                        port,
                        new PaymentObservedCustomerProjectionRequestFactory()
                );

        service.project(historicalReceived);

        assertEquals(
                ObservedCustomerProjectionRequest
                        .ProjectionPaymentStatus.RECEIVED,
                request.getValue().paymentStatus()
        );
    }

    private static ObservedCustomerProjectionEvent event(
            ProjectionPaymentStatus status
    ) {
        return ObservedCustomerProjectionEvent.versionOne(
                EVENT_ID,
                PAYMENT_ID,
                8,
                ObservedCustomerProjectionEventType
                        .PAYMENT_STATUS_CHANGED,
                new ObservedCustomerProjectionPayload(
                        "PAY-2026-000123",
                        "M0123456",
                        "Société ABC SARL",
                        "***-***-1234",
                        "a***@example.com",
                        "SIXPAY_BANK",
                        "v1:" + "a".repeat(64),
                        "•••• 1234",
                        new BigDecimal("15000.00"),
                        "XAF",
                        status,
                        null,
                        CREATED_AT,
                        OCCURRED_AT
                ),
                "c74e165f-df46-463e-a520-188e6df3e5ae",
                OCCURRED_AT
        );
    }
}
