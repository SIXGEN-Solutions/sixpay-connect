package com.sixpay.payment.application.event.projection;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ObservedCustomerProjectionEventTest {
    private static final UUID EVENT_ID = UUID.fromString(
            "11111111-1111-4111-8111-111111111111"
    );
    private static final UUID PAYMENT_ID = UUID.fromString(
            "7ed75090-8af7-4dfa-9b62-8e4dca73501a"
    );
    private static final String CORRELATION_ID =
            "c74e165f-df46-463e-a520-188e6df3e5ae";
    private static final Instant CREATED_AT =
            Instant.parse("2026-08-04T15:00:00Z");
    private static final Instant UPDATED_AT =
            Instant.parse("2026-08-04T15:01:00Z");

    @Test
    void createsCanonicalVersionOneEnvelope() {
        var event = ObservedCustomerProjectionEvent.versionOne(
                EVENT_ID,
                PAYMENT_ID,
                8,
                ObservedCustomerProjectionEventType.PAYMENT_REJECTED,
                rejectedPayload(),
                CORRELATION_ID,
                UPDATED_AT
        );
        assertEquals(1, event.eventVersion());
        assertEquals("PAYMENT", event.aggregateType());
        assertEquals(8, event.aggregateVersion());
    }

    @Test
    void rejectsUnsupportedVersionAndInvalidMetadata() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ObservedCustomerProjectionEvent(
                        EVENT_ID,
                        2,
                        PAYMENT_ID,
                        "PAYMENT",
                        1,
                        ObservedCustomerProjectionEventType.PAYMENT_CREATED,
                        receivedPayload(),
                        CORRELATION_ID,
                        CREATED_AT
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> ObservedCustomerProjectionEvent.versionOne(
                        EVENT_ID,
                        PAYMENT_ID,
                        0,
                        ObservedCustomerProjectionEventType.PAYMENT_CREATED,
                        receivedPayload(),
                        CORRELATION_ID,
                        CREATED_AT
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> ObservedCustomerProjectionEvent.versionOne(
                        EVENT_ID,
                        PAYMENT_ID,
                        1,
                        ObservedCustomerProjectionEventType.PAYMENT_CREATED,
                        receivedPayload(),
                        "not-a-uuid",
                        CREATED_AT
                )
        );
    }

    @Test
    void renderedFormProtectsPayload() {
        var event = ObservedCustomerProjectionEvent.versionOne(
                EVENT_ID,
                PAYMENT_ID,
                8,
                ObservedCustomerProjectionEventType.PAYMENT_REJECTED,
                rejectedPayload(),
                CORRELATION_ID,
                UPDATED_AT
        );
        String rendered = event.toString();
        assertFalse(rendered.contains("M0123456"));
        assertFalse(rendered.contains("Société ABC SARL"));
        assertFalse(rendered.contains("v1:" + "a".repeat(64)));
    }

    private static ObservedCustomerProjectionPayload receivedPayload() {
        return payload(ProjectionPaymentStatus.RECEIVED, null, CREATED_AT);
    }

    private static ObservedCustomerProjectionPayload rejectedPayload() {
        return payload(
                ProjectionPaymentStatus.REJECTED,
                "ACCOUNT_NOT_FOUND",
                UPDATED_AT
        );
    }

    private static ObservedCustomerProjectionPayload payload(
            ProjectionPaymentStatus status,
            String failureCode,
            Instant updatedAt
    ) {
        return new ObservedCustomerProjectionPayload(
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
                status,
                failureCode,
                CREATED_AT,
                updatedAt
        );
    }
}
