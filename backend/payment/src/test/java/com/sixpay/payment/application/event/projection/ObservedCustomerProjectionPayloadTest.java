package com.sixpay.payment.application.event.projection;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ObservedCustomerProjectionPayloadTest {
    private static final Instant AT =
            Instant.parse("2026-08-04T15:00:00Z");

    @Test
    void rejectsRawAccountAndInvalidFingerprint() {
        assertThrows(
                IllegalArgumentException.class,
                () -> payload(
                        "10005-00001-12345678901-12",
                        "v1:" + "a".repeat(64),
                        ProjectionPaymentStatus.RECEIVED,
                        null
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> payload(
                        "•••• 1234",
                        "invalid",
                        ProjectionPaymentStatus.RECEIVED,
                        null
                )
        );
    }

    @Test
    void enforcesFailureCodeSemantics() {
        assertThrows(
                IllegalArgumentException.class,
                () -> payload(
                        "•••• 1234",
                        "v1:" + "a".repeat(64),
                        ProjectionPaymentStatus.REJECTED,
                        null
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> payload(
                        "•••• 1234",
                        "v1:" + "a".repeat(64),
                        ProjectionPaymentStatus.RECEIVED,
                        "ACCOUNT_NOT_FOUND"
                )
        );
    }

    @Test
    void toStringProtectsSensitiveFields() {
        var payload = payload(
                "•••• 1234",
                "v1:" + "a".repeat(64),
                ProjectionPaymentStatus.RECEIVED,
                null
        );
        String rendered = payload.toString();
        assertFalse(rendered.contains("M0123456"));
        assertFalse(rendered.contains("Société ABC SARL"));
        assertFalse(rendered.contains("v1:" + "a".repeat(64)));
        assertFalse(rendered.contains("•••• 1234"));
    }

    @Test
    void amountUsesCanonicalContractScale() {
        ObservedCustomerProjectionPayload payload =
                new ObservedCustomerProjectionPayload(
                        "PAY-2026-000123",
                        "M0123456",
                        "Société ABC SARL",
                        "***-***-1234",
                        "a***@example.com",
                        "AMPLITUDE",
                        "v1:" + "a".repeat(64),
                        "•••• 1234",
                        new BigDecimal("15000"),
                        "XAF",
                        ProjectionPaymentStatus.RECEIVED,
                        null,
                        Instant.parse(
                                "2026-08-04T00:55:00Z"
                        ),
                        Instant.parse(
                                "2026-08-04T00:55:00Z"
                        )
                );

        assertEquals(
                new BigDecimal("15000.00"),
                payload.amount()
        );
    }

    private static ObservedCustomerProjectionPayload payload(
            String maskedAccount,
            String fingerprint,
            ProjectionPaymentStatus status,
            String failureCode
    ) {
        return new ObservedCustomerProjectionPayload(
                "PAY-2026-000123",
                "M0123456",
                "Société ABC SARL",
                "***-***-1234",
                "a***@example.com",
                "AMPLITUDE",
                fingerprint,
                maskedAccount,
                new BigDecimal("15000.00"),
                "XAF",
                status,
                failureCode,
                AT,
                AT
        );
    }

}
