package com.sixpay.notification.infrastructure.messaging;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JacksonPartnerStatusChangedEventDecoderTest {

    private final JacksonPartnerStatusChangedEventDecoder decoder =
            new JacksonPartnerStatusChangedEventDecoder(new ObjectMapper());

    @Test
    void decodesPartnerEventWithoutDependingOnPartnerModule() {
        var decoded = decoder.decode("""
                {
                  "schemaVersion": 2,
                  "eventId": "19516c06-ae79-4f94-b321-66823934b9ff",
                  "partnerId": "8ec6a427-406f-4f93-b271-cbc819a4c1dd",
                  "previousStatus": "PENDING_VALIDATION",
                  "currentStatus": "ACTIVE",
                  "reason": null,
                  "recipientEmail": "alice.ops@example.com",
                  "actorId": "manager@sixpay",
                  "correlationId": "corr-decision",
                  "occurredAt": "2026-07-27T12:00:00Z"
                }
                """);

        assertThat(decoded.currentStatus()).isEqualTo("ACTIVE");
        assertThat(decoded.recipientEmail())
                .isEqualTo("alice.ops@example.com");
    }

    @Test
    void rejectsMalformedPayload() {
        assertThatThrownBy(() -> decoder.decode("{"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid");
    }
}
