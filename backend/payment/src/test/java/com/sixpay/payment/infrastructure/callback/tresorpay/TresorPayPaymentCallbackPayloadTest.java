package com.sixpay.payment.infrastructure.callback.tresorpay;

import com.sixpay.payment.application.port.output.callback.PaymentStatusCallbackMessage;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TresorPayPaymentCallbackPayloadTest {

    @Test
    void mapsProviderNeutralReferenceToTresorPayWireField() {
        UUID eventId = UUID.fromString(
                "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
        );

        var message = new PaymentStatusCallbackMessage(
                "1.0",
                eventId,
                PaymentStatusCallbackMessage.CUT_CREDITED,
                Instant.parse("2026-08-03T10:31:00Z"),
                UUID.fromString(
                        "11111111-1111-1111-1111-111111111111"
                ),
                null,
                UUID.fromString(
                        "11111111-2222-3333-4444-555555555555"
                ),
                "PAY-1234567890ABCDEFGHJKMNPQRS",
                "AVI-2025-00045678",
                "LRB",
                2L,
                new PaymentStatusCallbackMessage.CutCreditedData(
                        new PaymentStatusCallbackMessage.MoneyData(
                                "1000",
                                "XAF"
                        ),
                        "BANK-EVENT-123",
                        Instant.parse("2026-08-03T10:31:00Z"),
                        "POSTED_PENDING_TFJ",
                        false
                )
        );

        var payload = TresorPayPaymentCallbackPayload.from(message);

        assertThat(message.externalPaymentReference())
                .isEqualTo("AVI-2025-00045678");
        assertThat(payload.tresorPayPaymentReference())
                .isEqualTo(message.externalPaymentReference());
        assertThat(payload.eventId())
                .isEqualTo(message.eventId());
        assertThat(payload.data())
                .isSameAs(message.data());
    }
}
