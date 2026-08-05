package com.sixpay.payment.infrastructure.outbox.serialization;

import com.sixpay.payment.application.event.projection
        .ObservedCustomerProjectionEvent;
import com.sixpay.payment.application.event.projection
        .ObservedCustomerProjectionEventType;
import com.sixpay.payment.application.event.projection
        .ObservedCustomerProjectionPayload;
import com.sixpay.payment.application.event.projection
        .ProjectionPaymentStatus;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentOutboxEventSerializationTest {

    private static final UUID EVENT_ID = UUID.fromString(
            "11111111-1111-4111-8111-111111111111"
    );

    private static final UUID PAYMENT_ID = UUID.fromString(
            "7ed75090-8af7-4dfa-9b62-8e4dca73501a"
    );

    private static final String CORRELATION_ID =
            "c74e165f-df46-463e-a520-188e6df3e5ae";

    private static final Instant CREATED_AT =
            Instant.parse("2026-08-04T00:55:00Z");

    private static final Instant OCCURRED_AT =
            Instant.parse("2026-08-04T01:00:00Z");

    @Test
    void roundTripPreservesVersionOneContractAndCanonicalIdentity() {
        ObjectMapper mapper = objectMapper();
        PaymentOutboxEventTypeRegistry registry =
                new PaymentOutboxEventTypeRegistry();

        PaymentOutboxEventSerializer serializer =
                new PaymentOutboxEventSerializer(
                        mapper,
                        registry
                );

        PaymentOutboxEventDeserializer deserializer =
                new PaymentOutboxEventDeserializer(
                        mapper,
                        registry
                );

        ObservedCustomerProjectionEvent source = event();

        String json = serializer.serialize(source);
        ObservedCustomerProjectionEvent restored =
                deserializer.deserialize(json);

        assertEquals(source, restored);
        assertEquals(EVENT_ID, restored.eventId());
        assertEquals(PAYMENT_ID, restored.paymentId());
        assertEquals(8L, restored.aggregateVersion());
        assertEquals(CORRELATION_ID, restored.correlationId());
        assertEquals(
                ObservedCustomerProjectionEvent
                        .CURRENT_EVENT_VERSION,
                restored.eventVersion()
        );

        assertTrue(json.contains(
                "\"eventType\":\"payment.observation-projection\""
        ));
        assertTrue(json.contains("\"eventVersion\":1"));
        assertTrue(json.contains("\"aggregateVersion\":8"));
        assertTrue(json.contains(
                "\"projectionEventType\":\"PAYMENT_REJECTED\""
        ));

        assertFalse(json.contains("com.sixpay.payment."));
        assertFalse(json.contains(
                "ObservedCustomerProjectionEvent\""
        ));
    }

    @Test
    void freshCodecInstancesReadPersistedVersionOneEvent() {
        String persistedJson =
                new PaymentOutboxEventSerializer(
                        objectMapper(),
                        new PaymentOutboxEventTypeRegistry()
                ).serialize(event());

        ObservedCustomerProjectionEvent restored =
                new PaymentOutboxEventDeserializer(
                        objectMapper(),
                        new PaymentOutboxEventTypeRegistry()
                ).deserialize(persistedJson);

        assertEquals(EVENT_ID, restored.eventId());
        assertEquals(PAYMENT_ID, restored.paymentId());
        assertEquals(8L, restored.aggregateVersion());
        assertEquals(CORRELATION_ID, restored.correlationId());
        assertEquals(
                ProjectionPaymentStatus.REJECTED,
                restored.payload().paymentStatus()
        );
    }

    @Test
    void unknownTypeIsRejectedAsContractError() {
        String json = serialize(event()).replace(
                "payment.observation-projection",
                "payment.unknown-contract"
        );

        assertThrows(
                UnknownPaymentOutboxEventTypeException.class,
                () -> deserialize(json)
        );
    }

    @Test
    void unsupportedVersionIsRejectedAsContractError() {
        String json = serialize(event()).replace(
                "\"eventVersion\":1",
                "\"eventVersion\":99"
        );

        assertThrows(
                UnsupportedPaymentOutboxEventVersionException.class,
                () -> deserialize(json)
        );
    }

    @Test
    void incompletePayloadIsRejectedWithoutLeakingPayload() {
        String json = serialize(event()).replace(
                "\"currency\":\"XAF\",",
                ""
        );

        PaymentOutboxSerializationException exception =
                assertThrows(
                        PaymentOutboxSerializationException.class,
                        () -> deserialize(json)
                );

        assertFalse(exception.getMessage().contains("M0123456"));
        assertFalse(exception.getMessage().contains(
                "Société ABC SARL"
        ));
        assertFalse(exception.getMessage().contains(
                "v1:" + "a".repeat(64)
        ));
    }

    @Test
    void serializedContractContainsNoRawBankingCredentialOrAccount() {
        String json = serialize(event());

        for (String forbidden : new String[] {
                "23700123456789012345678",
                "ribDebiteur",
                "accountNumber",
                "integrationAccountToken",
                "DebtorAccountReference",
                "Authorization",
                "Bearer ",
                "JWT",
                "apiKey",
                "clientSecret",
                "rawResponse"
        }) {
            assertFalse(
                    json.contains(forbidden),
                    () -> "Forbidden contract data: " + forbidden
            );
        }

        assertTrue(json.contains(
                "\"maskedAccountReference\":\"•••• 1234\""
        ));
        assertTrue(json.contains(
                "\"accountBindingFingerprint\":\"v1:"
        ));
    }

    @Test
    void envelopeAndEventRenderingProtectPayload() throws Exception {
        ObjectMapper mapper = objectMapper();
        String json = serialize(event());

        PaymentOutboxEventEnvelope envelope =
                mapper.readValue(
                        json,
                        PaymentOutboxEventEnvelope.class
                );

        for (String rendered : new String[] {
                envelope.toString(),
                event().toString()
        }) {
            assertFalse(rendered.contains("M0123456"));
            assertFalse(rendered.contains("Société ABC SARL"));
            assertFalse(rendered.contains(
                    "v1:" + "a".repeat(64)
            ));
            assertTrue(rendered.contains("payload=[PROTECTED]"));
        }
    }

    private static String serialize(
            ObservedCustomerProjectionEvent event
    ) {
        return new PaymentOutboxEventSerializer(
                objectMapper(),
                new PaymentOutboxEventTypeRegistry()
        ).serialize(event);
    }

    private static ObservedCustomerProjectionEvent deserialize(
            String json
    ) {
        return new PaymentOutboxEventDeserializer(
                objectMapper(),
                new PaymentOutboxEventTypeRegistry()
        ).deserialize(json);
    }

    private static ObjectMapper objectMapper() {
        return JsonMapper.builder()
                .findAndAddModules()
                .build();
    }

    private static ObservedCustomerProjectionEvent event() {
        return ObservedCustomerProjectionEvent.versionOne(
                EVENT_ID,
                PAYMENT_ID,
                8,
                ObservedCustomerProjectionEventType
                        .PAYMENT_REJECTED,
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
                        ProjectionPaymentStatus.REJECTED,
                        "ACCOUNT_NOT_FOUND",
                        CREATED_AT,
                        OCCURRED_AT
                ),
                CORRELATION_ID,
                OCCURRED_AT
        );
    }
}
