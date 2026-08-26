package com.sixpay.payment.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentOutboxSerializationArchitectureTest {

    private static final Path OUTBOX_ROOT = Path.of(
            "src/main/java/com/sixpay/payment/"
                    + "infrastructure/outbox"
    );

    @Test
    void stableRegistryNeverUsesJavaClassNamesAsContractIds()
            throws Exception {

        String registry = Files.readString(
                OUTBOX_ROOT.resolve(
                        "serialization/"
                                + "PaymentOutboxEventTypeRegistry.java"
                )
        );

        assertTrue(registry.contains(
                "\"payment.observation-projection\""
        ));

        for (String forbidden : List.of(
                "Class.forName(",
                "getClass().getName(",
                "getCanonicalName(",
                "com.sixpay.payment.domain.event."
        )) {
            assertFalse(
                    registry.contains(forbidden),
                    () -> "Registry contains unstable contract "
                            + "mechanism: "
                            + forbidden
            );
        }
    }

    @Test
    void serializerAndEnvelopeProtectOperationalRendering()
            throws Exception {

        String envelope = Files.readString(
                OUTBOX_ROOT.resolve(
                        "serialization/"
                                + "PaymentOutboxEventEnvelope.java"
                )
        );
        String serializer = Files.readString(
                OUTBOX_ROOT.resolve(
                        "serialization/"
                                + "PaymentOutboxEventSerializer.java"
                )
        );
        String deserializer = Files.readString(
                OUTBOX_ROOT.resolve(
                        "serialization/"
                                + "PaymentOutboxEventDeserializer.java"
                )
        );

        assertTrue(envelope.contains(
                "payload=[PROTECTED]"
        ));
        assertFalse(serializer.contains(
                "log."
        ));
        assertFalse(deserializer.contains(
                "log."
        ));
        assertFalse(serializer.contains(
                "getClass().getName("
        ));
        assertFalse(deserializer.contains(
                "Class.forName("
        ));
    }

    @Test
    void mapperCapturesEventMetadataAndSnapshotState()
            throws Exception {

        String mapper = Files.readString(
                OUTBOX_ROOT.resolve(
                        "mapper/"
                                + "PaymentObservedCustomerProjectionEventMapper.java"
                )
        ).replaceAll("\\s+", "");

        for (String required : List.of(
                "event.eventId()",
                "event.aggregateVersion()",
                "event.correlationId().value()",
                "event.occurredAt()",
                "state.receivedAt()",
                "state.updatedAt()",
                "state.debtorAccountReference()"
                        + ".bindingFingerprint()",
                "state.debtorAccountReference()"
                        + ".maskedDisplay()"
        )) {
            assertTrue(
                    mapper.contains(required),
                    () -> "Missing durable snapshot mapping: "
                            + required
            );
        }

        for (String forbidden : List.of(
                "import com.sixpay.customer.",
                "RestClient",
                "WebClient",
                "HttpClient",
                "PaymentOutboxEntity",
                "accountNumber",
                "ribDebiteur",
                "integrationAccountToken"
        )) {
            assertFalse(
                    mapper.contains(forbidden),
                    () -> "Projection mapper contains forbidden "
                            + "concept: "
                            + forbidden
            );
        }
    }
}
